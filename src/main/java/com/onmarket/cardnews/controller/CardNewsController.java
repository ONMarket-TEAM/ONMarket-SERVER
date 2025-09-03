package com.onmarket.cardnews.controller;
import lombok.extern.slf4j.Slf4j;

import com.onmarket.cardnews.dto.CardNewsRequest;
import com.onmarket.cardnews.dto.PosterConfig;
import com.onmarket.cardnews.dto.TargetType;
import com.onmarket.cardnews.dto.CreditLoanOptionDto;
import com.onmarket.cardnews.dto.SupportConditionDto;
import com.onmarket.cardnews.service.HtmlRenderService;
import com.onmarket.cardnews.service.HtmlTemplateService;
import com.onmarket.cardnews.service.OpenAIClientService;
import com.onmarket.cardnews.service.RowAssemblerService;
import com.onmarket.cardnews.service.S3Uploader;

import com.onmarket.fssdata.domain.CreditLoanOption;
import com.onmarket.fssdata.domain.CreditLoanProduct;
import com.onmarket.fssdata.repository.CreditLoanProductRepository;

import com.onmarket.loandata.domain.LoanProduct;
import com.onmarket.loandata.repository.LoanProductRepository;

import com.onmarket.supportsdata.domain.SupportCondition;
import com.onmarket.supportsdata.domain.SupportProduct;
import com.onmarket.supportsdata.repository.SupportProductRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.util.Base64;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cardnews")
@RequiredArgsConstructor
@Slf4j
public class CardNewsController {

    private final RowAssemblerService rowAssembler;
    private final OpenAIClientService openai;
    private final HtmlTemplateService htmlTemplateService;
    private final HtmlRenderService htmlRenderService;

    private final S3Uploader s3Uploader;
    private final LoanProductRepository loanProductRepository;
    private final CreditLoanProductRepository creditLoanProductRepository;
    private final SupportProductRepository supportProductRepository;

    private static final String DEFAULT_BG_PROMPT = String.join(" ",
            "public information poster layout, top half features friendly illustration or photo of diverse people",
            "(including disabled person with wheelchair, family, caregivers, guide dog) centered;",
            "bottom half bright and clean with subtle gradient and room for text;",
            "soft pastel background (light pink/beige), curved slogan arc above the group;",
            "modern cheerful flat style, inclusive, high quality, no text");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(
            @Valid @RequestBody CardNewsRequest req,
            @RequestParam(name = "targetType", required = false) TargetType targetType,
            @RequestParam(name = "targetId",   required = false) Long targetId,
            @RequestParam(name = "targetKey",  required = false) String targetKey
    ) {
        int w   = req.getWidth() == null ? 1024 : req.getWidth();
        int h   = req.getHeight() == null ? 1536 : req.getHeight();
        int dsf = req.getDeviceScaleFactor() == null ? 2 : req.getDeviceScaleFactor();
        boolean returnDataUrl = Boolean.TRUE.equals(req.getReturnDataUrl());

        // rowText 없고 targetType 있으면 DB에서 DTO 주입
        if ((req.getRowText() == null || req.getRowText().isBlank()) && targetType != null) {
            switch (targetType) {
                case LOAN_PRODUCT -> {
                    if (targetId == null) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "targetId is required when targetType=LOAN_PRODUCT and rowText is empty"));
                    }
                    LoanProduct e = loanProductRepository.findById(targetId).orElse(null);
                    if (e == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                                "error", "LoanProduct not found: id=" + targetId));
                    }
                    req.setLoanProduct(mapLoanProduct(e));
                }
                case CREDIT_LOAN_PRODUCT -> {
                    if (targetId == null) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "targetId is required when targetType=CREDIT_LOAN_PRODUCT and rowText is empty"));
                    }
                    CreditLoanProduct e = creditLoanProductRepository.findById(targetId).orElse(null);
                    if (e == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                                "error", "CreditLoanProduct not found: id=" + targetId));
                    }
                    req.setCreditLoanProduct(mapCreditLoanProduct(e));
                    if (e.getOptions() != null && !e.getOptions().isEmpty()) {
                        List<CreditLoanOptionDto> optionDtos = e.getOptions().stream()
                                .map(this::mapCreditLoanOption)
                                .collect(Collectors.toList());
                        req.setCreditLoanOptions(optionDtos);
                    }
                }
                case SUPPORT_SERVICE -> {
                    String serviceId = (targetKey != null && !targetKey.isBlank())
                            ? targetKey
                            : (targetId != null ? String.valueOf(targetId) : null);
                    if (serviceId == null) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "targetKey (serviceId) is required when targetType=SUPPORT_SERVICE and rowText is empty"));
                    }
                    SupportProduct e = supportProductRepository.findById(serviceId).orElse(null);
                    if (e == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                                "error", "SupportProduct not found: serviceId=" + serviceId));
                    }
                    req.setSupportService(mapSupportService(e));
                    if (e.getSupportCondition() != null) {
                        req.setSupportConditions(List.of(mapSupportCondition(e.getSupportCondition())));
                    }
                }
            }
        }

        log.info("[CardNews] request width={} height={} dsf={} targetType={} targetId={} targetKey={} returnDataUrl={}",
            w, h, dsf, targetType, targetId, targetKey, returnDataUrl);
        // 1) row 텍스트 조립
        String row = (req.getRowText() != null && !req.getRowText().isBlank())
                ? req.getRowText()
                : rowAssembler.toRowText(req);

        // 2) 요약(JSON) -> PosterConfig로 매핑
        String summaryJson = null;
        PosterConfig cfg;
        try {
            log.info("[CardNews] summarize start (row.length={})", (row == null ? 0 : row.length()));
            summaryJson = openai.summarize(row); // String(JSON) 기대
            log.info("[CardNews] summarize done ({} chars)", (summaryJson == null ? 0 : summaryJson.length()));
        } catch (Exception ex) {
            log.warn("[CardNews] summarize failed: {}", ex.toString());
        }
        try {
            cfg = MAPPER.readValue((summaryJson == null ? "" : summaryJson), PosterConfig.class);
        } catch (Exception parseEx) {
            // PosterConfig 구조에 맞춘 폴백 (섹션 중심)
            String excerpt = row.length() > 120 ? row.substring(0, 120) + "..." : row;
            PosterConfig.Section sec = PosterConfig.Section.builder()
                    .heading("핵심 요약")
                    .bullets(List.of(excerpt))
                    .text(null)
                    .build();
            cfg = PosterConfig.builder()
                    .title("요약")
                    .subtitle(null)
                    .badge(null)
                    .sections(List.of(sec))
                    .theme(null)
                    .build();
        }

        // 3) 배경 생성 사이즈 계산 + 프롬프트 맥락 결합
        boolean portrait = h >= w;
        String dalleSize = portrait ? "1024x1792" : "1792x1024";

        String basePrompt = (req.getBgPrompt() == null || req.getBgPrompt().isBlank())
                ? DEFAULT_BG_PROMPT
                : req.getBgPrompt();
        String prompt = basePrompt + " | context: " + buildSummaryHint(cfg);

        String bgDataUrl;
        try {
            log.info("[CardNews] image generate start size={} portrait={} prompt.len={}", dalleSize, portrait, (prompt == null ? 0 : prompt.length()));
            bgDataUrl = tryGenerateBackgroundDataUrlReflect(prompt, dalleSize, portrait, w, h);
            log.info("[CardNews] image generate done (bgDataUrl prefix={})", (bgDataUrl == null ? "null" : bgDataUrl.substring(0, Math.min(30, bgDataUrl.length()))));
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", (e.getMessage() == null ? e.getClass().getName() : e.getMessage()));
            err.put("requestedSize", dalleSize);
            log.warn("[CardNews] image generate bad request: {}", err);
            return ResponseEntity.badRequest().body(err);
        } catch (Exception e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "이미지 생성 중 오류가 발생했습니다.");
            err.put("detail", (e.getMessage() == null ? e.getClass().getName() : e.getMessage()));
            log.error("[CardNews] image generate error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }

        // 4) HTML 템플릿
        log.info("[CardNews] render html");
        String html = htmlTemplateService.renderHtml(cfg, bgDataUrl);

        // 5) PNG 렌더링
        byte[] png = htmlRenderService.renderToPngBytes(html, w, h, dsf);
        log.info("[CardNews] render png done ({} bytes)", (png == null ? -1 : png.length));

        // 6) dataURL만 반환
        if (returnDataUrl) {
            String b64 = java.util.Base64.getEncoder().encodeToString(png);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("data:image/png;base64," + b64);
        }

        // 7) S3 업로드
        var put = s3Uploader.uploadPng(png, null);

        // 8) 대상 엔티티 카드뉴스 필드 갱신(옵션)
        if (targetType != null) {
            Instant now = Instant.now();
            switch (targetType) {
                case LOAN_PRODUCT -> {
                    if (targetId == null) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "targetId is required for LOAN_PRODUCT"));
                    }
                    loanProductRepository.findById(targetId).ifPresent(e -> {
                        e.updateCardnews(put.key(), put.url(), now);
                        loanProductRepository.save(e);
                    });
                }
                case CREDIT_LOAN_PRODUCT -> {
                    if (targetId == null) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "targetId is required for CREDIT_LOAN_PRODUCT"));
                    }
                    creditLoanProductRepository.findById(targetId).ifPresent(e -> {
                        e.updateCardnews(put.key(), put.url(), now);
                        creditLoanProductRepository.save(e);
                    });
                }
                case SUPPORT_SERVICE -> {
                    String serviceId = (targetKey != null && !targetKey.isBlank())
                            ? targetKey
                            : (targetId != null ? String.valueOf(targetId) : null);
                    if (serviceId == null) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "targetKey (serviceId) is required for SUPPORT_SERVICE"));
                    }
                    supportProductRepository.findById(serviceId).ifPresent(e -> {
                        e.updateCardnews(put.key(), put.url(), now);
                        supportProductRepository.save(e);
                    });
                }
            }
        }

        // 9) 응답
        Map<String, Object> resp = new LinkedHashMap<>();
        if (put != null) {
            if (put.url() != null) resp.put("url", put.url());
            if (put.key() != null) resp.put("s3Key", put.key());
        }
        if (targetType != null) resp.put("targetType", targetType.toString());
        if (targetId != null)   resp.put("targetId", targetId);
        if (targetKey != null)  resp.put("targetKey", targetKey);

        return ResponseEntity.ok(resp);
    }

    // ---- 매퍼들 ----
    private com.onmarket.cardnews.dto.LoanProductDto mapLoanProduct(LoanProduct e) {
        return com.onmarket.cardnews.dto.LoanProductDto.builder()
                .id(e.getId())
                .sequence(parseIntSafe(e.getSequence()))
                .productName(e.getProductName())
                .usage(e.getUsage())
                .target(e.getTarget())
                .targetFilter(e.getTargetFilter())
                .institutionCategory(e.getInstitutionCategory())
                .offeringInstitution(e.getOfferingInstitution())
                .repaymentMethod(e.getRepaymentMethod())
                .interestCategory(e.getInterestCategory())
                .interestRate(e.getInterestRate())
                .loanLimit(e.getLoanLimit())
                .maxTotalTerm(parseIntSafe(e.getMaxTotalTerm()))
                .maxDeferredTerm(parseIntSafe(e.getMaxDeferredTerm()))
                .maxRepaymentTerm(parseIntSafe(e.getMaxRepaymentTerm()))
                .age(e.getAge())
                .ageBelow39(parseIntSafe(e.getAgeBelow39()))
                .specialTargetConditions(e.getSpecialTargetConditions())
                .otherReference(e.getOtherReference())
                .repaymentFee(e.getRepaymentFee())
                .relatedSite(e.getRelatedSite())
                .build();
    }

    private com.onmarket.cardnews.dto.CreditLoanProductDto mapCreditLoanProduct(CreditLoanProduct e) {
        return com.onmarket.cardnews.dto.CreditLoanProductDto.builder()
                .id(e.getId())
                .dclsMonth(e.getDclsMonth())
                .finCoNo(e.getFinCoNo())
                .korCoNm(e.getKorCoNm())
                .finPrdtCd(e.getFinPrdtCd())
                .finPrdtNm(e.getFinPrdtNm())
                .joinWay(e.getJoinWay())
                .crdtPrdtType(e.getCrdtPrdtType())
                .crdtPrdtTypeNm(e.getCrdtPrdtTypeNm())
                .cbName(e.getCbName())
                .dclsStrtDay(e.getDclsStrtDay())
                .dclsEndDay(e.getDclsEndDay())
                .finCoSubmDay(e.getFinCoSubmDay())
                .build();
    }

    private CreditLoanOptionDto mapCreditLoanOption(CreditLoanOption e) {
        return CreditLoanOptionDto.builder()
                .id(e.getId())
                .finCoNo(e.getFinCoNo())
                .finPrdtCd(e.getFinPrdtCd())
                .crdtLendRateType(e.getCrdtLendRateType())
                .crdtLendRateTypeNm(e.getCrdtLendRateTypeNm())
                .crdtGrad1(e.getCrdtGrad1())
                .crdtGrad4(e.getCrdtGrad4())
                .crdtGrad5(e.getCrdtGrad5())
                .crdtGrad6(e.getCrdtGrad6())
                .crdtGrad10(e.getCrdtGrad10())
                .crdtGrad11(e.getCrdtGrad11())
                .crdtGrad12(e.getCrdtGrad12())
                .crdtGrad13(e.getCrdtGrad13())
                .crdtGradAvg(e.getCrdtGradAvg())
                .build();
    }

    private com.onmarket.cardnews.dto.SupportServiceDto mapSupportService(SupportProduct e) {
        return com.onmarket.cardnews.dto.SupportServiceDto.builder()
                .serviceId(e.getServiceId())
                .supportType(e.getSupportType())
                .serviceName(e.getServiceName())
                .servicePurposeSummary(e.getServicePurposeSummary())
                .supportTarget(e.getSupportTarget())
                .selectionCriteria(e.getSelectionCriteria())
                .supportContent(e.getSupportContent())
                .applicationMethod(e.getApplicationMethod())
                .detailUrl(e.getDetailUrl())
                .departmentName(e.getDepartmentName())
                .userCategory(e.getUserCategory())
                .servicePurpose(e.getServicePurpose())
                .applicationDeadline(e.getApplicationDeadline())
                .requiredDocuments(e.getRequiredDocuments())
                .receptionAgencyName(e.getReceptionAgencyName())
                .contact(e.getContact())
                .onlineApplicationUrl(e.getOnlineApplicationUrl())
                .laws(e.getLaws())
                .keywords(e.getKeywords())
                .build();
    }

    private SupportConditionDto mapSupportCondition(SupportCondition e) {
        return SupportConditionDto.builder()
                .id(e.getId())
                .genderMale(e.getGenderMale())
                .genderFemale(e.getGenderFemale())
                .ageStart(e.getAgeStart())
                .ageEnd(e.getAgeEnd())
                .incomeBracket1(e.getIncomeBracket1())
                .incomeBracket2(e.getIncomeBracket2())
                .incomeBracket3(e.getIncomeBracket3())
                .incomeBracket4(e.getIncomeBracket4())
                .incomeBracket5(e.getIncomeBracket5())
                .jobEmployee(e.getJobEmployee())
                .jobSeeker(e.getJobSeeker())
                .householdSinglePerson(e.getHouseholdSinglePerson())
                .householdMultiChild(e.getHouseholdMultiChild())
                .householdNoHome(e.getHouseholdNoHome())
                .businessProspective(e.getBusinessProspective())
                .businessOperating(e.getBusinessOperating())
                .businessStruggling(e.getBusinessStruggling())
                .build();
    }

    private Integer parseIntSafe(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.valueOf(s.trim()); }
        catch (NumberFormatException ex) { return null; }
    }

    @GetMapping("/presigned")
    public ResponseEntity<?> getPresigned(@RequestParam String key) {
        String url = s3Uploader.presignedGetUrl(
                "onmarket-userfiles",
                key,
                Duration.ofMinutes(30)
        );
        return ResponseEntity.ok(Map.of("presignedUrl", url));
    }

    // ---------------------------
    // 요약 힌트 (DALLE 프롬프트용) - PosterConfig 기반
    // ---------------------------
    private String buildSummaryHint(PosterConfig cfg) {
        if (cfg == null) return "";
        StringBuilder sb = new StringBuilder();
        try {
            if (cfg.getTitle() != null && !cfg.getTitle().isBlank()) {
                sb.append("title=").append(cfg.getTitle()).append("; ");
            }
        } catch (Throwable ignore) {}

        try {
            if (cfg.getBadge() != null && !cfg.getBadge().isBlank()) {
                sb.append("badge=").append(cfg.getBadge()).append("; ");
            }
        } catch (Throwable ignore) {}

        try {
            if (cfg.getSections() != null && !cfg.getSections().isEmpty()) {
                PosterConfig.Section first = cfg.getSections().get(0);
                if (first.getHeading() != null && !first.getHeading().isBlank()) {
                    sb.append("section=").append(first.getHeading()).append("; ");
                }
                if (first.getBullets() != null && !first.getBullets().isEmpty()) {
                    var top = first.getBullets().stream().limit(2).collect(Collectors.toList());
                    sb.append("bullets=").append(String.join(", ", top)).append("; ");
                }
            }
        } catch (Throwable ignore) {}

        String s = sb.toString().trim();
        return s.length() > 300 ? s.substring(0, 300) : s;
    }

    /**
     * 다양한 OpenAIClientService 시그니처를 리플렉션으로 호환 호출.
     * 우선순위:
     * 1) generateBackgroundDataUrl(String prompt, String size, boolean portrait)
     * 2) generateBackgroundDataUrl(String prompt, int width, int height)
     * 3) generateBackground(String prompt, int width, int height)
     * 4) generateBackground(String prompt, String size)
     */
    private String tryGenerateBackgroundDataUrlReflect(String prompt, String size, boolean portrait,
                                                       int width, int height) throws Exception {
        Class<?> cls = openai.getClass();
        try {
            Method m = cls.getMethod("generateBackgroundDataUrl", String.class, String.class, boolean.class);
            Object r = m.invoke(openai, prompt, size, portrait);
            return castToDataUrl(r);
        } catch (NoSuchMethodException ignore) {}

        try {
            Method m = cls.getMethod("generateBackgroundDataUrl", String.class, int.class, int.class);
            Object r = m.invoke(openai, prompt, width, height);
            return castToDataUrl(r);
        } catch (NoSuchMethodException ignore) {}

        try {
            Method m = cls.getMethod("generateBackground", String.class, int.class, int.class);
            Object r = m.invoke(openai, prompt, width, height);
            return castToDataUrl(r);
        } catch (NoSuchMethodException ignore) {}

        try {
            Method m = cls.getMethod("generateBackground", String.class, String.class);
            Object r = m.invoke(openai, prompt, size);
            return castToDataUrl(r);
        } catch (NoSuchMethodException ignore) {}

        // 최후 폴백: 빈 투명 PNG dataURL
        byte[] empty = new byte[0];
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(empty);
    }

    private String castToDataUrl(Object r) {
        if (r == null) return null;
        if (r instanceof String s) {
            // 서비스가 이미 dataURL을 반환하면 그대로 사용
            if (s.startsWith("data:image")) return s;
            // raw S3 URL 또는 임시 키를 반환하는 구현도 있을 수 있음(템플릿에서 그대로 씀)
            return s;
        }
        if (r instanceof byte[] bytes) {
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        }
        throw new IllegalArgumentException("지원하지 않는 반환 타입: " + r.getClass());
    }
}
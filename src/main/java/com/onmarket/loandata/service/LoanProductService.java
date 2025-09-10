package com.onmarket.loandata.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.onmarket.loandata.domain.LoanProduct;
import com.onmarket.loandata.dto.XmlLoanApiResponse;
import com.onmarket.loandata.repository.LoanProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanProductService {

    private final LoanProductRepository loanProductRepository;
    private final RestTemplate restTemplate;
    private final XmlMapper xmlMapper = new XmlMapper();

    @Value("${gov.api.loan.service-key}")
    private String serviceKey;

    @Value("${gov.api.loan.base-url}")
    private String baseUrl;

    private static final List<String> REGIONAL_KEYWORDS = List.of(
            "서울시", "서울", "부산시", "부산", "대구시", "대구", "인천시", "인천", "광주시", "광주",
            "대전시", "대전", "울산시", "울산", "세종시", "세종", "경기도", "경기", "강원특별자치도",
            "강원자치도", "강원도", "강원", "충청북도", "충북", "충청남도", "충남", "전라북도", "전북",
            "전라남도", "전남", "경상북도", "경북", "경상남도", "경남", "제주특별자치도", "제주도", "제주",
            "울주군", "북구", "남구", "동구", "중구", "수성구", "달성군", "광산구", "서구", "원주시",
            "청주시", "충주시", "제천시", "옥천군", "영동군", "단양군", "보은군", "증평군", "정읍시",
            "익산시", "순창군", "진안군", "김제시", "고창군", "완주군", "군산시", "전주시", "부안군",
            "진주시", "함양군", "고양시", "구리시", "김포시", "남양주시", "동두천시", "연천군",
            "양주시", "양평군", "의정부시", "파주시", "포천시", "성남시", "수원시", "안성시", "용인시",
            "의왕시", "안산시", "시흥시", "하남시", "이천시", "평택시", "광명시", "화성시", "남양주시",
            "구리시", "여주시"
    );

    // 모든 대출 상품 데이터 수집
    @Transactional
    public void fetchAndSaveAllLoanProducts() {
        int pageNo = 1;
        int numOfRows = 100;
        int totalProcessed = 0;

        do {
            try {
                log.info("대출 상품 데이터 수집 시작 - 페이지: {}", pageNo);
                XmlLoanApiResponse response = fetchLoanProducts(pageNo, numOfRows);
                if (response == null || response.getHeader() == null || !"00".equals(response.getHeader().getResultCode())) {
                    log.error("API 오류 또는 응답 없음: {}", response != null && response.getHeader() != null ? response.getHeader().getResultMsg() : "응답 없음");
                    break;
                }
                if (response.getBody() == null || response.getBody().getItems() == null || response.getBody().getItems().getItem() == null) {
                    log.info("수집할 데이터가 더 이상 없습니다.");
                    break;
                }

                List<XmlLoanApiResponse.XmlLoanItem> items = response.getBody().getItems().getItem();
                log.info("페이지 {}에서 {}개 데이터 조회", pageNo, items.size());
                saveXmlLoanProducts(items);

                totalProcessed += items.size();
                log.info("페이지 {} 처리 완료, 누적 처리: {}개", pageNo, totalProcessed);

                if (items.size() < numOfRows) {
                    log.info("모든 데이터 수집 완료 - 총 {}개", totalProcessed);
                    break;
                }
                pageNo++;
                Thread.sleep(1000);

            } catch (Exception e) {
                log.error("데이터 수집 중 오류 발생: ", e);
                break;
            }
        } while (true);
    }

    // XML 대출 상품 API 호출
    private XmlLoanApiResponse fetchLoanProducts(int pageNo, int numOfRows) throws Exception {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/LoanProductSearchingInfo/getLoanProductSearchingInfo")
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows)
                .build()
                .toUriString();

        log.debug("API 호출 URL: {}", url);
        String xmlResponse = restTemplate.getForObject(url, String.class);

        if (xmlResponse == null || xmlResponse.trim().isEmpty()) {
            log.error("API로부터 빈 응답을 받았습니다.");
            return null;
        }
        return xmlMapper.readValue(xmlResponse, XmlLoanApiResponse.class);
    }

    // XML 대출 상품 데이터 저장 (필터링 적용)
    @Transactional
    public void saveXmlLoanProducts(List<XmlLoanApiResponse.XmlLoanItem> items) {
        List<LoanProduct> productsToSave = new ArrayList<>();

        items.forEach(item -> {
            // 필터링 조건: target 또는 specialTargetConditions에 특정 키워드 포함 시만 저장
            String target = item.getTrgt() != null ? item.getTrgt() : "";
            String suprTgtDtlCond = item.getSuprTgtDtlCond() != null ? item.getSuprTgtDtlCond() : "";

            if (target.contains("사업자") || target.contains("기업") || target.contains("소상공인") || target.contains("청년 창업자") ||
                    suprTgtDtlCond.contains("사업자") || suprTgtDtlCond.contains("기업") || suprTgtDtlCond.contains("소상공인") || suprTgtDtlCond.contains("청년 창업자")) {

                try {
                    Optional<LoanProduct> existingProduct = loanProductRepository.findBySequence(item.getSeq());

                    LoanProduct product;
                    if (existingProduct.isPresent()) {
                        // 기존 상품 업데이트
                        product = existingProduct.get();
                        updateExistingProduct(item, product);
                        // 새로운 키워드 컬럼 업데이트
                        String keywords = extractKeywordsFromItem(item);
                        product.setKeywords(keywords);
                        log.debug("기존 상품 업데이트: {}", item.getSeq());
                    } else {
                        // 새 상품 생성 - @Builder 사용
                        product = createNewProduct(item);
                        // 새로운 키워드 컬럼 생성
                        String keywords = extractKeywordsFromItem(item);
                        product.setKeywords(keywords);
                        log.debug("새 상품 생성: {}", item.getSeq());
                    }

                    productsToSave.add(product);
                } catch (Exception e) {
                    log.error("상품 매핑 실패: {} - {}", item.getSeq(), e.getMessage());
                }
            } else {
                log.debug("필터링 조건 미충족 - 저장하지 않음: {}", item.getSeq());
            }
        });

        if (!productsToSave.isEmpty()) {
            loanProductRepository.saveAll(productsToSave);
            log.info("필터링 후 대출 상품 정보 저장 완료 - {}개", productsToSave.size());
        } else {
            log.info("필터링 후 저장할 상품 없음");
        }
    }

    // 새 상품 생성 - @Builder 패턴 사용
    private LoanProduct createNewProduct(XmlLoanApiResponse.XmlLoanItem item) {
        String applicationUrl = generateApplicationUrl(item.getOfrInstNm());
        String keywords = extractKeywordsFromItem(item); // 키워드 추출

        return LoanProduct.builder()
                .sequence(item.getSeq())
                .productName(truncateString(item.getFinPrdNm(), 255))
                .loanLimit(item.getLnLmt())
                .limitOver1000(item.getLnLmt1000Abnml())
                .limitOver2000(item.getLnLmt2000Abnml())
                .limitOver3000(item.getLnLmt3000Abnml())
                .limitOver5000(item.getLnLmt5000Abnml())
                .limitOver10000(item.getLnLmt10000Abnml())
                .interestCategory(item.getIrtCtg())
                .interestRate(item.getIrt())
                .maxTotalTerm(item.getMaxTotLnTrm())
                .maxDeferredTerm(item.getMaxDfrmTrm())
                .maxRepaymentTerm(item.getMaxRdptTrm())
                .repaymentMethod(item.getRdptMthd())
                .usage(item.getUsge())
                .target(item.getTrgt())
                .institutionCategory(item.getInstCtg())
                .offeringInstitution(item.getOfrInstNm())
                .specialTargetConditions(item.getSuprTgtDtlCond())
                .age(item.getAge())
                .ageBelow39(item.getAge39Blw())
                .income(item.getIncm())
                .handlingInstitution(item.getHdlInst())
                .relatedSite(applicationUrl)
                .keywords(keywords) // 빌더에 키워드 추가
                .build();
    }

    // 기존 상품 업데이트 - 비즈니스 메서드 사용
    private void updateExistingProduct(XmlLoanApiResponse.XmlLoanItem item, LoanProduct product) {
        if (product.getRelatedSite() == null || product.getRelatedSite().isEmpty()) {
            String applicationUrl = generateApplicationUrl(item.getOfrInstNm());
            product.setRelatedSite(applicationUrl);
        }

        // 업데이트 메서드에 키워드 필드 추가
        String keywords = extractKeywordsFromItem(item);
        product.setKeywords(keywords);

        product.updateFromXmlData(
                item.getSeq(),
                truncateString(item.getFinPrdNm(), 255),
                item.getLnLmt(),
                item.getLnLmt1000Abnml(),
                item.getLnLmt2000Abnml(),
                item.getLnLmt3000Abnml(),
                item.getLnLmt5000Abnml(),
                item.getLnLmt10000Abnml(),
                item.getIrtCtg(),
                item.getIrt(),
                item.getMaxTotLnTrm(),
                item.getMaxDfrmTrm(),
                item.getMaxRdptTrm(),
                item.getRdptMthd(),
                item.getUsge(),
                item.getTrgt(),
                item.getInstCtg(),
                item.getOfrInstNm(),
                item.getSuprTgtDtlCond(),
                item.getAge(),
                item.getAge39Blw(),
                item.getIncm(),
                item.getHdlInst()
        );
    }
    // 키워드 추출 로직 추가
    private String extractKeywordsFromItem(XmlLoanApiResponse.XmlLoanItem item) {
        StringBuilder extractedKeywords = new StringBuilder();

        // ofr_inst_nm, supr_tgt_dtl_cond, fin_prd_nm 세 컬럼에서 지역 키워드 추출
        String textToAnalyze = (item.getOfrInstNm() != null ? item.getOfrInstNm() : "") + " " +
                (item.getSuprTgtDtlCond() != null ? item.getSuprTgtDtlCond() : "") + " " +
                (item.getFinPrdNm() != null ? item.getFinPrdNm() : "");

        List<String> foundRegions = REGIONAL_KEYWORDS.stream()
                .filter(region -> Pattern.compile("\\b" + Pattern.quote(region) + "\\b").matcher(textToAnalyze).find())
                .collect(Collectors.toList());

        if (!foundRegions.isEmpty()) {
            // 중복 제거 및 쉼표로 연결
            extractedKeywords.append(String.join(",", foundRegions.stream().distinct().collect(Collectors.toList())));
        }

        // 추가적인 공통 키워드
        List<String> commonKeywords = new ArrayList<>();
        if (textToAnalyze.contains("소상공인")) commonKeywords.add("소상공인");
        if (textToAnalyze.contains("기업")) commonKeywords.add("기업");
        if (textToAnalyze.contains("사업자")) commonKeywords.add("사업자");
        if (textToAnalyze.contains("창업")) commonKeywords.add("창업");
        if (textToAnalyze.contains("청년")) commonKeywords.add("청년");

        if (!commonKeywords.isEmpty()) {
            if (extractedKeywords.length() > 0) {
                extractedKeywords.append(",");
            }
            extractedKeywords.append(String.join(",", commonKeywords.stream().distinct().collect(Collectors.toList())));
        }

        return extractedKeywords.length() > 0 ? extractedKeywords.toString() : null;
    }

    private String truncateString(String str, int maxLength) {
        if (str == null) return null;
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    private String generateApplicationUrl(String institutionName) {
        if (institutionName == null) return null;

        // 서민금융진흥원
        if (institutionName.contains("서민금융진흥원") ||
                institutionName.contains("미소금융") ||
                institutionName.contains("서민금융")) {
            return "https://www.kinfa.or.kr/main.do";
        }

        // 신용보증재단
        if (institutionName.contains("신용보증재단")) return "https://www.koreg.or.kr/";

        // 개별 신용보증재단
        if (institutionName.contains("서울신용보증재단")) return "https://www.seoulshinbo.co.kr/";
        if (institutionName.contains("울산신용보증재단")) return "https://www.ulsanshinbo.co.kr/main/";
        if (institutionName.contains("전남신용보증재단")) return "https://www.jnsinbo.or.kr/jnsinbo/intro.do";
        if (institutionName.contains("대전신용보증재단")) return "https://www.sinbo.or.kr/";
        if (institutionName.contains("광주신용보증재단")) return "https://www.gjsinbo.or.kr/";
        if (institutionName.contains("부산신용보증재단")) return "https://www.busansinbo.or.kr/main.do";
        if (institutionName.contains("전북신용보증재단")) return "https://www.jbcredit.or.kr/";
        if (institutionName.contains("충북신용보증재단")) return "https://www.cbsig.or.kr/";
        if (institutionName.contains("충남신용보증재단")) return "https://www.cbsinbo.or.kr/";
        if (institutionName.contains("강원신용보증재단")) return "https://www.gwsinbo.or.kr/main/intro.php";
        if (institutionName.contains("경기신용보증재단")) return "https://www.gcgf.or.kr/gcgf/intro.do";
        if (institutionName.contains("경남신용보증재단")) return "https://www.gnsinbo.or.kr/";
        if (institutionName.contains("경북신용보증재단")) return "https://gbsinbo.co.kr/";
        if (institutionName.contains("대구신용보증재단")) return "https://www.ttg.co.kr/";
        if (institutionName.contains("세종신용보증재단")) return "https://www.sjsinbo.or.kr/";
        if (institutionName.contains("인천신용보증재단")) return "https://www.icsinbo.or.kr/";
        if (institutionName.contains("제주신용보증재단")) return "https://www.jcgf.or.kr/index2.php";

        // 은행 및 기타
        if (institutionName.contains("신협")) return "https://www.cu.co.kr/";
        if (institutionName.contains("BNK경남은행")) return "https://www.bnksum.co.kr/";
        if (institutionName.contains("IBK기업은행")) return "https://www.knbank.co.kr/ib20/mnu/BHP000000000001";
        if (institutionName.contains("국민은행")) return "https://www.kbstar.com";
        if (institutionName.contains("신한은행")) return "https://www.shinhan.com";
        if (institutionName.contains("우리은행")) return "https://www.wooribank.com";
        if (institutionName.contains("하나은행")) return "https://www.kebhana.com/";

        // 기타 보증기관
        if (institutionName.contains("SGI서울보증")) return "https://www.sgic.co.kr/";

        // 기본 URL
        return "https://www.gov.kr/portal/onestopSvc/lonGoods";
    }

    // --- 검색 및 조회 메서드들 ---

    public long getTotalProductCount() {
        return loanProductRepository.count();
    }

    public List<LoanProduct> searchByProductName(String productName) {
        return loanProductRepository.findByProductNameContaining(productName);
    }

    public List<LoanProduct> getAllProducts(int page, int size) {
        return loanProductRepository.findAll(org.springframework.data.domain.PageRequest.of(page, size)).getContent();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDataStatus() {
        Map<String, Object> status = new HashMap<>();
        long totalProducts = loanProductRepository.count();

        status.put("totalProducts", totalProducts);
        status.put("isEmpty", totalProducts == 0);
        status.put("timestamp", System.currentTimeMillis());

        return status;
    }

    @Transactional(readOnly = true)
    public LoanProduct getProductBySeq(String seq) {
        return loanProductRepository.findBySequence(seq)
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다. SEQ: " + seq));
    }
}
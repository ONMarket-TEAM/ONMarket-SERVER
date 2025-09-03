package com.onmarket.caption.service;

import com.onmarket.caption.dto.CaptionRequest;
import com.onmarket.caption.dto.CaptionResponse;
import com.onmarket.caption.dto.OptionStyle;
import com.onmarket.caption.util.EmojiStripper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaptionService {

    private final S3TempStorageService s3Service;
    private final OpenAIClientService openai;

    public CaptionResponse createCaptions(CaptionRequest req) {
        List<String> uploadedKeys = new ArrayList<>();
        List<String> imageUrls = new ArrayList<>();

        try {
            // 1) 이미지 수집
            if (req.getFiles() != null && !req.getFiles().isEmpty()) {
                if (req.getFiles().size() > 10) throw new IllegalArgumentException("이미지는 최대 10장까지 가능합니다.");
                var ups = s3Service.uploadTempAll(req.getFiles());
                uploadedKeys = ups.stream().map(S3TempStorageService.Uploaded::key).toList();
                imageUrls   = ups.stream().map(S3TempStorageService.Uploaded::url).toList();
            } else if (req.getS3Urls() != null && !req.getS3Urls().isEmpty()) {
                if (req.getS3Urls().size() > 10) throw new IllegalArgumentException("이미지는 최대 10장까지 가능합니다.");
                imageUrls = req.getS3Urls();
            } else {
                throw new IllegalArgumentException("파일 목록(files) 또는 s3Urls 중 하나는 제공되어야 합니다.");
            }

            // 2) 옵션/프롬프트
            OptionStyle opt = req.getOptions() != null ? req.getOptions() : new OptionStyle();
            if (StringUtils.isBlank(opt.getTone())) opt.setTone("트렌디");
            if (StringUtils.isBlank(opt.getLanguage())) opt.setLanguage("ko");
            opt.setWithEmojis(false); // 이모지 절대 금지 강제

            final String systemPrompt = buildSystemPrompt(opt);
            final String userPrompt   = buildUserPrompt(opt, req.getContextHint(), req.getMustInclude());

            // 3) OpenAI 호출 (안정성 위해 temp=0.7)
            List<String> raw = openai.generateCaptionsFromImages(
                    imageUrls, systemPrompt, userPrompt, Math.max(1, opt.getVariations()), 0.7
            );

            // 4) 백업 안전장치: 이모지 제거(혹시라도 생기면)
            List<String> candidates = raw.stream()
                    .map(EmojiStripper::strip)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();

            // 5) (파일 업로드의 경우) 즉시 삭제
            boolean deleted = true;
            if (!uploadedKeys.isEmpty()) deleted = s3Service.deleteAll(uploadedKeys);

            return CaptionResponse.builder()
                    .imageUrls(imageUrls)
                    .candidates(candidates)
                    .toneApplied(opt.getTone())
                    .languageApplied(opt.getLanguage())
                    .deleted(deleted)
                    .build();

        } catch (Exception e) {
            log.error("createCaptions error", e);
            // GlobalExceptionHandler 에서 처리
            throw e;
        }
    }

    private String buildSystemPrompt(OptionStyle opt) {
        return """
            너는 소상공인/브랜드용 인스타그램 캡션을 만드는 카피라이터다.
            - 톤: %s
            - 언어: %s
            - 절대 규칙:
              1) 이모지(emoji) 사용 금지.
              2) 사용자 제공 '필수 문구'가 있으면, 철자/띄어쓰기까지 동일하게 포함할 것.
              3) Markdown/코드블록/인용부호 없이 순수 텍스트만 출력.
            - 사진의 구체 요소를 반영하여 스크롤을 멈추게 하는 간결한 문장으로 작성.
            """.formatted(opt.getTone(), opt.getLanguage());
    }

    private String buildUserPrompt(OptionStyle opt, String contextHint, String mustInclude) {
        StringBuilder sb = new StringBuilder();
        sb.append("다음 여러 이미지를 종합 분석해 인스타그램 캡션 후보들을 만들어줘.\n");
        sb.append("제약:\n");
        if (opt.getMaxChars() != null) sb.append("- 각 후보는 ").append(opt.getMaxChars()).append("자 이내.\n");
        else sb.append("- 각 후보는 1~2문장 위주, 약 140자 내외.\n");
        sb.append(opt.isWithHashtags() ? "- 적절한 해시태그 3~6개 포함.\n" : "- 해시태그는 포함하지 않는다.\n");
        sb.append("- 너무 일반적/상투적 표현은 피하고, 이미지의 구체 요소를 반영.\n");
        sb.append("- 출력은 후보 문장만 나열(번호/불릿/빈 줄 없이).\n");
        if (StringUtils.isNotBlank(mustInclude)) {
            sb.append("\n[필수 문구]\n").append(mustInclude).append("\n");
            sb.append("- 모든 후보는 위 [필수 문구]를 원문 그대로 포함해야 한다.\n");
        }
        if (StringUtils.isNotBlank(contextHint)) {
            sb.append("\n[참고 맥락]\n").append(contextHint).append("\n");
        }
        sb.append("\n이제 위 조건을 충족하는 캡션 후보들을 생성해줘.");
        return sb.toString();
    }
}
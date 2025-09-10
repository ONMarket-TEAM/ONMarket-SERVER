package com.onmarket.caption.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.onmarket.caption.dto.CaptionGenerateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaptionService {
    private final S3TempStorageService s3;
    private final OpenAIClientService ai;

    /** 파일을 받아 임시로 올리고, AI 호출 후 즉시 삭제 */
    public CaptionGenerateResponse generateFromFileAndDelete(MultipartFile file, String prompt) {
        String key = s3.uploadTemp(file);
        try {
            String getUrl = s3.presignGetUrl(key, 60); // 1분짜리 GET URL
            OpenAIClientService.Generated g = ai.callCaptionWithImageUrl(getUrl, prompt);
            return toResponse(g, s3.delete(key));
        } finally {
            // 예외 시에도 삭제 시도
            s3.delete(key);
        }
    }

    /** 이미 프런트가 올린 s3Key를 받아 AI 호출 후 삭제 */
    public CaptionGenerateResponse generateFromS3AndDelete(String s3Key, String prompt) {
        try {
            String getUrl = s3.presignGetUrl(s3Key, 60);
            OpenAIClientService.Generated g = ai.callCaptionWithImageUrl(getUrl, prompt);
            return toResponse(g, s3.delete(s3Key));
        } finally {
            s3.delete(s3Key);
        }
    }

    /** 다중 s3Keys를 받아 첫 3장만 분석하여 AI 호출 후 모든 이미지 삭제 */
    public CaptionGenerateResponse generateFromMultipleS3AndDelete(List<String> s3Keys, String prompt) {
        boolean allDeleted = true;

        try {
            if (s3Keys == null || s3Keys.isEmpty()) {
                throw new IllegalArgumentException("s3Keys 리스트가 비어있습니다");
            }

            // 최대 3개까지만 분석 (안전장치)
            List<String> keysToAnalyze = s3Keys.size() > 3
                    ? s3Keys.subList(0, 3)
                    : new ArrayList<>(s3Keys);

            log.info("총 {}개 이미지 중 앞의 {}개를 분석합니다: {}",
                    s3Keys.size(), keysToAnalyze.size(), keysToAnalyze);

            // 분석할 이미지들의 presigned URL 생성
            List<String> imageUrls = new ArrayList<>();
            for (String key : keysToAnalyze) {
                try {
                    String getUrl = s3.presignGetUrl(key, 60);
                    imageUrls.add(getUrl);
                    log.debug("이미지 URL 생성 성공: {}", key);
                } catch (Exception e) {
                    log.error("이미지 URL 생성 실패: {}", key, e);
                    throw new RuntimeException("이미지 URL 생성 실패: " + key, e);
                }
            }

            // OpenAI 다중 이미지 분석 호출
            OpenAIClientService.Generated g = ai.callCaptionWithMultipleImageUrls(imageUrls, prompt);

            return toResponse(g, allDeleted);

        } finally {
            // 모든 이미지 삭제 (분석한 것뿐만 아니라 업로드된 모든 이미지)
            for (String key : s3Keys) {
                try {
                    boolean deleted = s3.delete(key);
                    if (!deleted) {
                        allDeleted = false;
                        log.warn("이미지 삭제 실패: {}", key);
                    } else {
                        log.debug("이미지 삭제 성공: {}", key);
                    }
                } catch (Exception e) {
                    allDeleted = false;
                    log.error("이미지 삭제 중 예외 발생: {}", key, e);
                }
            }

            log.info("총 {}개 이미지 삭제 완료 (성공: {})", s3Keys.size(), allDeleted);
        }
    }

    private static CaptionGenerateResponse toResponse(OpenAIClientService.Generated g, boolean deleted) {
        List<String> tags = new ArrayList<>();
        JsonNode arr = g.hashtags();
        if (arr != null && arr.isArray()) {
            arr.forEach(n -> tags.add(n.asText()));
        }
        return CaptionGenerateResponse.builder()
                .caption(g.caption())
                .hashtags(tags)
                .bestPostTime(g.bestPostTime())
                .impactNote(g.impactNote())
                .sourceDeleted(deleted)
                .build();
    }
}
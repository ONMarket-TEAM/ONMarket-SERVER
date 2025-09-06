package com.onmarket.caption.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.onmarket.caption.dto.CaptionGenerateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

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
        // null 체크 및 디버깅 로그 추가
        System.out.println("=== CaptionService 디버깅 ===");
        System.out.println("받은 s3Key: '" + s3Key + "'");
        System.out.println("받은 prompt: '" + prompt + "'");

        if (s3Key == null || s3Key.trim().isEmpty()) {
            System.out.println("s3Key가 null이거나 비어있음");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "s3Key가 필요합니다");
        }

        if (prompt == null || prompt.trim().isEmpty()) {
            System.out.println("prompt가 null이거나 비어있음");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prompt가 필요합니다");
        }

        try {
            System.out.println("presignGetUrl 호출 시도 - s3Key: " + s3Key);
            String getUrl = s3.presignGetUrl(s3Key, 60);

            if (getUrl == null) {
                System.out.println("presignGetUrl이 null을 반환함");
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 URL 생성 실패");
            }

            System.out.println("AI 호출 시도 - getUrl: " + getUrl);
            OpenAIClientService.Generated g = ai.callCaptionWithImageUrl(getUrl, prompt);

            System.out.println("S3 파일 삭제 시도");
            boolean deleted = s3.delete(s3Key);

            return toResponse(g, deleted);
        } catch (Exception e) {
            System.out.println("에러 발생: " + e.getMessage());
            // 에러 발생 시에도 삭제 시도
            try {
                s3.delete(s3Key);
            } catch (Exception deleteError) {
                System.out.println("삭제 중 에러: " + deleteError.getMessage());
            }
            throw e;
        }
    }

    private static CaptionGenerateResponse toResponse(OpenAIClientService.Generated g, boolean deleted) {
        List<String> tags = new ArrayList<>();
        JsonNode arr = g.hashtags();
        if (arr != null && arr.isArray()) arr.forEach(n -> tags.add(n.asText()));
        return CaptionGenerateResponse.builder()
                .caption(g.caption())
                .hashtags(tags)
                .bestPostTime(g.bestPostTime())
                .impactNote(g.impactNote())
                .sourceDeleted(deleted)
                .build();
    }
}
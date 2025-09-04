package com.onmarket.caption.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.onmarket.caption.dto.CaptionGenerateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
        try {
            String getUrl = s3.presignGetUrl(s3Key, 60);
            OpenAIClientService.Generated g = ai.callCaptionWithImageUrl(getUrl, prompt);
            return toResponse(g, s3.delete(s3Key));
        } finally {
            s3.delete(s3Key);
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
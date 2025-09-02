package com.onmarket.member.service;

import com.onmarket.s3.dto.PresignPutResponse;

public interface ProfileImageService {
    PresignPutResponse presignForUpload(Long memberId, String filename, String contentType);

    ImageUrlResponse confirmUpload(Long memberId, String key);

    ImageUrlResponse current(Long memberId);

    void deleteProfileImage(Long memberId);

    // 응답 DTO (프로필 전용 간단 DTO)
    @lombok.Value
    class ImageUrlResponse {
        String key;
        String url; // presigned GET url
    }
}

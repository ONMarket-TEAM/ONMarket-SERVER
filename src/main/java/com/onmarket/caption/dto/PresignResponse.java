package com.onmarket.caption.dto;

import lombok.AllArgsConstructor; import lombok.Data;

@Data @AllArgsConstructor
public class PresignResponse {
    private String url; // PUT presigned URL
    private String key; // s3 object key
    private String publicUrl; // 퍼블릭 버킷일 경우만 반환 (아니면 null)
}
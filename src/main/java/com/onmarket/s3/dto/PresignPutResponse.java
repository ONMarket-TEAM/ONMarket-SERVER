package com.onmarket.s3.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PresignPutResponse {
    @Schema(description = "S3로 직접 PUT할 수 있는 URL")
    private final String uploadUrl;

    @Schema(description = "업로드된 객체를 식별할 최종 key", example = "uploads/uuid-test.png")
    private final String key;

    @Schema(description = "업로드 시 사용할 Content-Type", example = "image/png")
    private final String contentType;
}
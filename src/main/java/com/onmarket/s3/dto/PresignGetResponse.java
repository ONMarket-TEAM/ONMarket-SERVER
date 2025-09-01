package com.onmarket.s3.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PresignGetResponse {
    @Schema(description = "제한 시간 동안 접근 가능한 다운로드 URL")
    private final String downloadUrl;
}
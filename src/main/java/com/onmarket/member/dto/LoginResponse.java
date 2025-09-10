package com.onmarket.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    @Schema(description = "Access Token (API 요청 시 인증용 토큰)",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "Refresh Token (Access Token 갱신용)",
            example = "dGhpc2lzYXJlZnJlc2h0b2tlbi4uLg==")
    private String refreshToken;
}

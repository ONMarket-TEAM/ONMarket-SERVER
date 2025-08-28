package com.onmarket.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshRequest {

    @Schema(description = "갱신용 Refresh Token",
            example = "dGhpc2lzYXJlZnJlc2h0b2tlbi4uLg==")
    private String refreshToken;
}

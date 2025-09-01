package com.onmarket.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordCodeVerifyRequest {

    @Schema(description = "회원 이메일(로그인 시 아이디)", example = "test@test.com")
    private String email;

    @Schema(description = "인증코드", example = "123456")
    private String code;
}

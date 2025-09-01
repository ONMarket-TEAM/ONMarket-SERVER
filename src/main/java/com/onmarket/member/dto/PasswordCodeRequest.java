package com.onmarket.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class PasswordCodeRequest {

    @Schema(description = "회원 이메일(로그인 시 아이디)", example = "test@test.com")
    private String email;
}

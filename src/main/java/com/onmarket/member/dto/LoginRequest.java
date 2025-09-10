package com.onmarket.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @Schema(description = "사용자 이메일", example = "test@test.com")
    private String email;

    @Schema(description = "사용자 비밀번호", example = "123456789!")
    private String password;
}

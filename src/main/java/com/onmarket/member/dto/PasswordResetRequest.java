package com.onmarket.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class PasswordResetRequest {

    @Schema(description = "회원 이메일(로그인 시 아이디)", example = "test@test.com")
    private String email;

    @Schema(description = "비밀번호 재설정", example = "123123123!")
    private String newPassword;

    @Schema(description = "비밀번호 재설정 확인", example = "123123123!")
    private String confirmNewPassword;
}

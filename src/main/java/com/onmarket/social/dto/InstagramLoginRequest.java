package com.onmarket.social.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstagramLoginRequest {
    @Schema(description = "인스타 로그인 아이디", example = "insta_user")
    private String username;

    @Schema(description = "인스타 비밀번호(가짜)", example = "123456789")
    private String password;
}

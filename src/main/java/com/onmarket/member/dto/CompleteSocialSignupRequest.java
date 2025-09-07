package com.onmarket.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompleteSocialSignupRequest {

    @Schema(description = "사용자 닉네임 (필수)", example = "onmarket_user")
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2-20자 사이여야 합니다.")
    private String nickname;

    @Schema(description = "프로필 이미지 파일 키 (선택값, S3 업로드 시 반환되는 Key)", example = "profile/123456789.png")
    private String profileImageKey;
}

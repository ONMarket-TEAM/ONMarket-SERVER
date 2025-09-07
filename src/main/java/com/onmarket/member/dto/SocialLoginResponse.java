package com.onmarket.member.dto;

import com.onmarket.member.domain.enums.MemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SocialLoginResponse {

    @Schema(description = "회원 ID", example = "1001")
    private Long memberId;

    @Schema(description = "사용자 이메일", example = "user@example.com")
    private String email;

    @Schema(description = "닉네임", example = "onmarket_user")
    private String nickname;

    @Schema(description = "프로필 이미지 URL", example = "https://cdn.onmarket.com/profile/1001.png")
    private String profileImage;

    @Schema(
            description = "회원 상태 (PENDING: 추가정보 필요, ACTIVE: 로그인 완료)",
            example = "PENDING"
    )
    private MemberStatus status;

    @Schema(
            description = "Access Token (status가 ACTIVE일 때만 포함됨)",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    private String accessToken;

    @Schema(
            description = "Refresh Token (status가 ACTIVE일 때만 포함됨)",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    private String refreshToken;
}

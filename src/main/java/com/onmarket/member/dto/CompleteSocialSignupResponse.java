package com.onmarket.member.dto;

import com.onmarket.member.domain.enums.MemberStatus;
import com.onmarket.member.domain.enums.Gender;
import com.onmarket.member.domain.enums.SocialProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompleteSocialSignupResponse {

    @Schema(description = "회원 ID", example = "1001")
    private Long memberId;

    @Schema(description = "사용자 계정명 (소셜에서 제공된 기본 username)", example = "kakao_123456")
    private String username;

    @Schema(description = "사용자 이메일", example = "user@example.com")
    private String email;

    @Schema(description = "닉네임", example = "onmarket_user")
    private String nickname;

    @Schema(description = "휴대폰 번호", example = "01012345678")
    private String phone;

    @Schema(description = "생년월일", example = "1995-08-15")
    private LocalDate birthDate;

    @Schema(description = "성별", example = "MALE")
    private Gender gender;

    @Schema(description = "프로필 이미지 URL", example = "https://cdn.onmarket.com/profile/1001.png")
    private String profileImage;

    @Schema(description = "인스타그램 사용자명", example = "insta_user123")
    private String instagramUsername;

    @Schema(description = "회원 상태", example = "ACTIVE")
    private MemberStatus status;

    @Schema(description = "소셜 로그인 타입", example = "KAKAO")
    private SocialProvider socialProvider;

    @Schema(description = "Access Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;
}

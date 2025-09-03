package com.onmarket.member.dto;

import com.onmarket.member.domain.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class MemberResponse {

    @Schema(description = "회원 ID", example = "1")
    private final Long memberId;

    @Schema(description = "회원 이름", example = "testuser")
    private final String username;

    @Schema(description = "회원 닉네임", example = "홍길동")
    private final String nickname;

    @Schema(description = "회원 이메일(로그인 시 아이디)", example = "test@test.com")
    private final String email;

    @Schema(description = "회원 전화번호", example = "010-1234-5678")
    private final String phone;

    @Schema(description = "회원 생년월일", example = "1995-08-27")
    private final LocalDate birthDate;

    @Schema(description = "회원 프로필사진", example = "https://example.com/profile.jpg")
    private final String profileImage;

    @Schema(description = "Instagram 계정명 (@ 포함)", example = "@onmarket_official")
    private final String instagramUsername;

    @Schema(description = "Instagram 연결 여부", example = "false")
    private final Boolean instagramConnected;

    public static MemberResponse from(Member m) {
        return MemberResponse.builder()
                .memberId(m.getMemberId())
                .username(m.getUsername())
                .nickname(m.getNickname())
                .email(m.getEmail())
                .phone(m.getPhone())
                .birthDate(m.getBirthDate())
                .profileImage(m.getProfileImage())
                .instagramUsername(m.getDisplayInstagramUsername())
                .instagramConnected(m.hasInstagramConnected())
                .build();
    }
}

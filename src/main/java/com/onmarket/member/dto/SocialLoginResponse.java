package com.onmarket.member.dto;

import com.onmarket.member.domain.enums.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SocialLoginResponse {
    private Long memberId;
    private String email;
    private String nickname;
    private String profileImage;
    private MemberStatus status;  // PENDING 또는 ACTIVE

    // status가 ACTIVE일 때만 포함되는 필드들
    private String accessToken;
    private String refreshToken;
}
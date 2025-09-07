package com.onmarket.member.dto;

import com.onmarket.member.domain.enums.MemberStatus;
import com.onmarket.member.domain.enums.Gender;
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
    private Long memberId;
    private String username;
    private String email;
    private String nickname;
    private String phone;
    private LocalDate birthDate;
    private Gender gender;
    private String profileImage;
    private String instagramUsername;
    private MemberStatus status;
    private String accessToken;
    private String refreshToken;
}
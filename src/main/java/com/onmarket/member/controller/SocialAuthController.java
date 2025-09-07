package com.onmarket.member.controller;

import com.onmarket.business.exception.BusinessException;
import com.onmarket.common.response.ApiResponse;
import com.onmarket.common.response.ResponseCode;
import com.onmarket.member.domain.enums.MemberStatus;
import com.onmarket.member.dto.CompleteSocialSignupResponse;
import com.onmarket.member.dto.CompleteSocialSignupRequest;
import com.onmarket.member.dto.SocialLoginResponse;
import com.onmarket.member.dto.SocialUserInfo;
import com.onmarket.member.repository.MemberRepository;
import com.onmarket.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import com.onmarket.member.domain.Member;


@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
@Slf4j
public class SocialAuthController {

    private final MemberService memberService;
    private final MemberRepository memberRepository;

    @GetMapping("/kakao/success")
    public ApiResponse<SocialLoginResponse> kakaoLoginSuccess(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User)) {
            return ApiResponse.fail(ResponseCode.OAUTH2_LOGIN_FAILED);
        }

        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            SocialUserInfo info = SocialUserInfo.fromKakao(oAuth2User);
            SocialLoginResponse response = memberService.handleSocialLogin(info);

            // 응답 메시지를 status에 따라 다르게 설정
            ResponseCode responseCode = response.getStatus() == MemberStatus.PENDING
                    ? ResponseCode.OAUTH2_ADDITIONAL_INFO_REQUIRED
                    : ResponseCode.LOGIN_SUCCESS;

            return ApiResponse.success(responseCode, response);
        } catch (BusinessException e) {
            log.error("Kakao login failed", e);
            return ApiResponse.fail(e.getResponseCode());
        }
    }

//    @GetMapping("/google/success")
//    public ApiResponse<SocialLoginResponse> googleLoginSuccess(Authentication authentication) {
//        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User)) {
//            return ApiResponse.fail(ResponseCode.OAUTH2_LOGIN_FAILED);
//        }
//
//        try {
//            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
//            SocialUserInfo info = SocialUserInfo.fromGoogle(oAuth2User);
//            SocialLoginResponse response = memberService.handleSocialLogin(info);
//
//            ResponseCode responseCode = response.getStatus() == MemberStatus.PENDING
//                    ? ResponseCode.OAUTH2_ADDITIONAL_INFO_REQUIRED
//                    : ResponseCode.LOGIN_SUCCESS;
//
//            return ApiResponse.success(responseCode, response);
//        } catch (BusinessException e) {
//            log.error("Google login failed", e);
//            return ApiResponse.fail(e.getResponseCode());
//        }
//    }

    @PostMapping("/complete-signup")
    public ApiResponse<CompleteSocialSignupResponse> completeSocialSignup(
            @RequestParam Long memberId,
            @RequestBody CompleteSocialSignupRequest request) {
        try {
            CompleteSocialSignupResponse response = memberService.completeSocialSignup(
                    memberId, request.getNickname(), request.getProfileImageKey());
            return ApiResponse.success(ResponseCode.SIGNUP_SUCCESS, response);
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getResponseCode());
        }
    }

    @GetMapping("/pending-member/{memberId}")
    public ApiResponse<CompleteSocialSignupResponse> getPendingMember(@PathVariable Long memberId) {
        try {
            Member member = memberService.findById(memberId);

            CompleteSocialSignupResponse response = CompleteSocialSignupResponse.builder()
                    .memberId(member.getMemberId())
                    .username(member.getUsername())
                    .email(member.getEmail())
                    .nickname(member.getNickname())
                    .phone(member.getPhone())
                    .birthDate(member.getBirthDate())
                    .gender(member.getGender())
                    .profileImage(member.getProfileImage())
                    .instagramUsername(member.getInstagramUsername())
                    .status(member.getStatus())
                    .build();

            return ApiResponse.success(ResponseCode.MEMBER_INFO_SUCCESS, response);
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getResponseCode());
        }
    }

}

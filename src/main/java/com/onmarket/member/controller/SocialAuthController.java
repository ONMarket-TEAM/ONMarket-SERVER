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

@io.swagger.v3.oas.annotations.tags.Tag(
        name = "소셜 로그인 API",
        description = "카카오/구글 등 OAuth2 기반 소셜 로그인 및 회원가입 관련 API"
)
@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
@Slf4j
public class SocialAuthController {

    private final MemberService memberService;
    private final MemberRepository memberRepository;

    @GetMapping("/kakao/success")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "카카오 로그인 성공 처리",
            description = "카카오 인증 성공 후 사용자 정보를 바탕으로 로그인 또는 추가 정보 요청을 처리합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공 또는 추가 정보 필요",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "OAuth2 인증 실패"
            )
    })
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

    @GetMapping("/google/success")
    public ApiResponse<SocialLoginResponse> googleLoginSuccess(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User)) {
            return ApiResponse.fail(ResponseCode.OAUTH2_LOGIN_FAILED);
        }

        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            SocialUserInfo info = SocialUserInfo.fromGoogle(oAuth2User);
            SocialLoginResponse response = memberService.handleSocialLogin(info);

            ResponseCode responseCode = response.getStatus() == MemberStatus.PENDING
                    ? ResponseCode.OAUTH2_ADDITIONAL_INFO_REQUIRED
                    : ResponseCode.LOGIN_SUCCESS;

            return ApiResponse.success(responseCode, response);
        } catch (BusinessException e) {
            log.error("Google login failed", e);
            return ApiResponse.fail(e.getResponseCode());
        }
    }



    @PostMapping("/complete-signup")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "소셜 회원가입 완료",
            description = "추가 정보를 입력해 소셜 회원가입을 최종 완료합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "회원가입 성공",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터"
            )
    })    public ApiResponse<CompleteSocialSignupResponse> completeSocialSignup(
            @RequestParam Long memberId,
            @RequestBody CompleteSocialSignupRequest request) {
        try {
            CompleteSocialSignupResponse response = memberService.completeSocialSignup(
                    memberId, request.getNickname(), request.getProfileImageKey(), request.getUsername(), request.getPhone(), request.getBirthDate(), request.getGender()                                                                                                                                                                          );
            return ApiResponse.success(ResponseCode.SIGNUP_SUCCESS, response);
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getResponseCode());
        }
    }

    @GetMapping("/pending-member/{memberId}")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "대기 중 회원 정보 조회",
            description = "추가 회원가입 정보가 필요한(PENDING 상태) 사용자의 기본 정보를 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "회원 정보 조회 성공",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "해당 회원을 찾을 수 없음"
            )
    })    public ApiResponse<CompleteSocialSignupResponse> getPendingMember(@PathVariable Long memberId) {
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
                    .socialProvider(member.getSocialProvider())
                    .build();

            return ApiResponse.success(ResponseCode.MEMBER_INFO_SUCCESS, response);
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getResponseCode());
        }
    }

}

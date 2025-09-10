package com.onmarket.member.controller;

import com.onmarket.business.exception.BusinessException;
import com.onmarket.common.response.ApiResponse;
import com.onmarket.common.response.ResponseCode;
import com.onmarket.member.dto.*;
import com.onmarket.member.service.AuthSessionService;
import com.onmarket.member.service.MemberService;
import com.onmarket.member.service.ProfileImageService;
import com.onmarket.s3.dto.PresignPutResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@Tag(name = "Member API", description = "회원 정보 관련 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/members/me")
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;
    private final AuthSessionService authSessionService;
    private final ProfileImageService profileImageService;

    private static final String AUTH_COOKIE_NAME = "profile_auth_token";
    private static final Duration AUTH_COOKIE_TTL = Duration.ofMinutes(10);

    @Value("${app.security.cookies.secure:false}")
    private boolean cookieSecure;

    @Value("${app.security.cookies.same-site:Strict}")
    private String cookieSameSite;

    @GetMapping
    @Operation(summary = "현재 회원정보 조회", description = "현재 로그인한 회원의 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음")
    })
    public ApiResponse<MemberResponse> me(
            @AuthenticationPrincipal String email
    ) {
        MemberResponse body = MemberResponse.from(memberService.findByEmail(email));
        return ApiResponse.success(ResponseCode.MEMBER_INFO_SUCCESS, body);
    }

    @PostMapping("/password/verify")
    @Operation(summary = "현재 비밀번호 확인", description = "현재 비밀번호가 일치하는지 확인합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비밀번호 검증 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "소셜 계정은 비밀번호 검증 불가")
    })
    public ApiResponse<Void> verifyPassword(
            @AuthenticationPrincipal String email,
            @RequestBody PasswordVerifyRequest request,
            HttpServletResponse response
    ) {
        memberService.verifyPassword(email, request.getCurrentPassword());

        // 검증 성공 시 임시 인증 토큰 생성
        String authToken = authSessionService.createAuthSession(email);

        // HttpOnly 쿠키로 토큰 전달
        ResponseCookie cookie = ResponseCookie.from(AUTH_COOKIE_NAME, authToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite(cookieSameSite)     // "Strict" / "Lax" / "None" (None은 Secure 필수)
                .maxAge(AUTH_COOKIE_TTL)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ApiResponse.success(ResponseCode.CURRENT_PASSWORD_VERIFY_SUCCESS, null);
    }


    @PatchMapping
    @Operation(summary = "회원정보 수정", description = "닉네임 또는 비밀번호를 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "검증 실패(닉네임/비밀번호 규칙 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "소셜 계정은 비밀번호 변경 불가")
    })
    public ApiResponse<MemberResponse> update(
            @AuthenticationPrincipal String email,
            @RequestBody MemberUpdateRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        // 임시 인증 토큰 검증
        boolean needsPasswordAuth = hasText(request.getNewPassword()) || hasText(request.getConfirmNewPassword());

        if (needsPasswordAuth) {
            // 일반 계정인지 확인
            if (memberService.findByEmail(email).getSocialProvider() != null) {
                throw new BusinessException(ResponseCode.SOCIAL_ACCOUNT_PASSWORD_CHANGE_NOT_ALLOWED);
            }

            // 쿠키에서 임시 인증 토큰 확인
            String authToken = getAuthTokenFromCookie(httpRequest);
            String authenticatedEmail = authSessionService.validateAuthSession(authToken);

            if (authenticatedEmail == null || !email.equals(authenticatedEmail)) {
                throw new BusinessException(ResponseCode.AUTH_TOKEN_NOT_FOUND);
            }

            // 인증 토큰 사용 후 무효화 (일회성)
            authSessionService.invalidateAuthSession(authToken);

            ResponseCookie clear = ResponseCookie.from(AUTH_COOKIE_NAME, "")
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .sameSite(cookieSameSite)
                    .maxAge(0)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, clear.toString());
        }

        MemberResponse body = MemberResponse.from(memberService.updateMember(email, request));
        return ApiResponse.success(ResponseCode.UPDATE_PROFILE_SUCCESS, body);
    }

    @PostMapping("/profile-image/presign")
    @Operation(
            summary = "프로필 이미지 업로드용 presigned URL 발급",
            description = "파일명/콘텐츠타입을 받아 S3 PUT presigned URL을 발급합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    public ApiResponse<PresignPutResponse> presignForProfileImage(
            @AuthenticationPrincipal String email,
            @RequestBody ProfileImagePresignRequest request
    ) {
        Long memberId = memberService.findByEmail(email).getMemberId(); // Member 엔티티의 PK 가정: getId()
        PresignPutResponse body = profileImageService.presignForUpload(
                memberId, request.getFilename(), request.getContentType()
        );
        // 전용 코드가 있다면 교체: e.g. ResponseCode.PROFILE_IMAGE_PRESIGN_SUCCESS
        return ApiResponse.success(ResponseCode.MEMBER_INFO_SUCCESS, body);
    }

    @PostMapping("/profile-image/confirm")
    @Operation(
            summary = "프로필 이미지 업로드 확정",
            description = "클라이언트가 S3에 업로드를 마친 후, 해당 key를 프로필 이미지로 반영합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "반영 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음")
    })
    public ApiResponse<ProfileImageService.ImageUrlResponse> confirmProfileImage(
            @AuthenticationPrincipal String email,
            @RequestBody ProfileImageConfirmRequest request
    ) {
        Long memberId = memberService.findByEmail(email).getMemberId();
        ProfileImageService.ImageUrlResponse body = profileImageService.confirmUpload(memberId, request.getKey());
        // 전용 코드가 있다면 교체: e.g. ResponseCode.PROFILE_IMAGE_UPDATE_SUCCESS
        return ApiResponse.success(ResponseCode.UPDATE_PROFILE_SUCCESS, body);
    }

    @GetMapping("/profile-image")
    @Operation(
            summary = "현재 프로필 이미지 조회",
            description = "회원의 현재 프로필 이미지 S3 key와 presigned GET URL을 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음")
    })
    public ApiResponse<ProfileImageService.ImageUrlResponse> getCurrentProfileImage(
            @AuthenticationPrincipal String email
    ) {
        Long memberId = memberService.findByEmail(email).getMemberId();
        ProfileImageService.ImageUrlResponse body = profileImageService.current(memberId);
        // 전용 코드가 있다면 교체: e.g. ResponseCode.PROFILE_IMAGE_GET_SUCCESS
        return ApiResponse.success(ResponseCode.MEMBER_INFO_SUCCESS, body);
    }

    @DeleteMapping("/profile-image")
    @Operation(
            summary = "프로필 이미지 삭제 (기본 이미지로 설정)",
            description = "프로필 이미지를 삭제하고 기본 이미지를 사용하도록 설정합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음")
    })
    public ApiResponse<Void> deleteProfileImage(
            @AuthenticationPrincipal String email
    ) {
        Long memberId = memberService.findByEmail(email).getMemberId();
        profileImageService.deleteProfileImage(memberId);
        return ApiResponse.success(ResponseCode.PROFILE_IMAGE_DELETE_SUCCESS, null);
    }

    // ===== 내부 유틸 =====
    /**
     * 쿠키에서 인증 토큰 추출
     */
    private String getAuthTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if (AUTH_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}

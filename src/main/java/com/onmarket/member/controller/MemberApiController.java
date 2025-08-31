package com.onmarket.member.controller;

import com.onmarket.common.response.ApiResponse;
import com.onmarket.common.response.ResponseCode;
import com.onmarket.member.dto.MemberResponse;
import com.onmarket.member.dto.MemberUpdateRequest;
import com.onmarket.member.dto.PasswordVerifyRequest;
import com.onmarket.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Member API", description = "회원 정보 관련 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/members/me")
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

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
            @RequestBody PasswordVerifyRequest request
    ) {
        memberService.verifyPassword(email, request.getCurrentPassword());
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
            @RequestBody MemberUpdateRequest request
    ) {
        MemberResponse body = MemberResponse.from(memberService.updateMember(email, request));
        return ApiResponse.success(ResponseCode.UPDATE_PROFILE_SUCCESS, body);
    }
}

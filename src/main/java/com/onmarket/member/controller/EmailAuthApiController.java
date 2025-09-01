package com.onmarket.member.controller;

import com.onmarket.common.response.ResponseCode;
import com.onmarket.member.dto.PasswordCodeVerifyRequest;
import com.onmarket.member.dto.PasswordResetRequest;
import com.onmarket.member.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import com.onmarket.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Email Auth API", description = "이메일 인증 관련 API")
@RestController
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
public class EmailAuthApiController {

    private final EmailService emailService;

    @GetMapping("/sendCode")
    @Operation(summary = "이메일 인증코드 발송", description = "비밀번호 찾기를 위한 이메일 인증코드 발송 API")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증코드 발송 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 이메일 형식"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회원"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "메일 발송 실패")
    })
    public ApiResponse<Void> sendVerificationCode(@RequestParam String email) {
        emailService.sendVerificationCode(email);
        return ApiResponse.success(ResponseCode.VERIFICATION_MAIL_SENT, null);
    }

    @PostMapping("/verifyCode")
    @Operation(summary = "이메일 인증코드 검증", description = "이메일로 발송된 인증코드를 인증하는 API")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증코드 검증 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회원 또는 만료된 인증코드"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "인증코드 불일치")
    })
    public ApiResponse<Void> verifyCode(@RequestBody PasswordCodeVerifyRequest request) {
        emailService.verifyEmailCode(request.getEmail(), request.getCode());
        return ApiResponse.success(ResponseCode.VERIFIED_CODE, null);
    }

    @PostMapping("/resetPassword")
    @Operation(summary = "비밀번호 재설정", description = "이메일 인증 완료 후 비밀번호 재설정하는 API")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비밀번호 재설정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회원"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "비밀번호 확인 불일치")
    })
    public ApiResponse<Void> resetPassword(@RequestBody PasswordResetRequest request) {
        emailService.resetPassword(request.getEmail(), request.getNewPassword(), request.getConfirmNewPassword());
        return ApiResponse.success(ResponseCode.PASSWORD_RESET_SUCCESS, null);
    }
}

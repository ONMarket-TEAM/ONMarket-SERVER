package com.onmarket.member.controller;

import com.onmarket.member.dto.SmsRequest;
import com.onmarket.member.dto.SmsVerifyRequest;
import com.onmarket.member.service.impl.SmsServiceImpl;
import com.onmarket.common.response.ApiResponse;
import com.onmarket.common.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.tags.Tag(
        name = "SMS API",
        description = "SMS 인증 관련 API"
)
@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SmsApiController {

    private final SmsServiceImpl smsService;

    @PostMapping("/verify-code")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "인증번호 발송",
            description = "입력한 휴대폰 번호로 인증번호를 발송합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "인증번호 발송 성공",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 휴대폰 번호 형식"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "SMS 발송 실패"
            )
    })
    public ApiResponse<?> sendVerifyCode(@RequestBody SmsRequest request) {
        smsService.sendVerifyCode(request.getPhoneNumber());
        return ApiResponse.success(ResponseCode.SMS_SEND_SUCCESS);
    }

    @PostMapping("/confirm-code")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "인증번호 확인",
            description = "입력한 인증번호가 올바른지 확인합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "인증 성공",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "인증번호 불일치"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "만료되었거나 존재하지 않는 인증번호"
            )
    })
    public ApiResponse<?> confirmVerifyCode(@RequestBody SmsVerifyRequest request) {
        boolean isValid = smsService.verifyCode(request.getPhoneNumber(), request.getCode());

        if (isValid) {
            return ApiResponse.success(ResponseCode.SMS_VERIFY_SUCCESS, true);
        } else {
            return ApiResponse.fail(ResponseCode.SMS_VERIFY_FAILED, false);
        }
    }
}

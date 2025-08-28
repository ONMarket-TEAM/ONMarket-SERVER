package com.onmarket.member.controller;

import com.onmarket.member.service.ValidationService;
import com.onmarket.common.response.ApiResponse;
import com.onmarket.common.response.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Validation API", description = "회원가입 시 중복 검증 관련 API")
@RestController
@RequestMapping("/api/validation")
@RequiredArgsConstructor
public class ValidationApiController {

    private final ValidationService validationService;

    // 이메일 중복 체크
    @GetMapping("/check/email")
    @Operation(summary = "이메일 중복 체크", description = "회원가입 시 이메일이 이미 사용 중인지 확인합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "200", description = "사용 가능한 이메일"),
            @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "409", description = "이미 사용 중인 이메일")
    })
    public ApiResponse<?> checkEmail(@RequestParam String email) {
        validationService.validateEmail(email);
        return ApiResponse.success(ResponseCode.VALID_EMAIL);
    }

    // 닉네임 중복 체크
    @GetMapping("/check/nickname")
    @Operation(summary = "닉네임 중복 체크", description = "회원가입 시 닉네임이 이미 사용 중인지 확인합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "200", description = "사용 가능한 닉네임"),
            @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "409", description = "이미 사용 중인 닉네임")
    })
    public ApiResponse<?> checkNickname(@RequestParam String nickname) {
        validationService.validateNickname(nickname);
        return ApiResponse.success(ResponseCode.VALID_NICKNAME);
    }
}

package com.onmarket.member.controller;

import com.onmarket.member.service.impl.ValidationService;
import com.onmarket.response.ApiResponse;
import com.onmarket.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/validation")
@RequiredArgsConstructor
public class ValidationApiController {

    private final ValidationService validationService;

    // 이메일 중복 체크
    @GetMapping("/check/email")
    public ApiResponse<?> checkEmail(@RequestParam String email) {
        validationService.validateEmail(email);
        return ApiResponse.success(ResponseCode.VALID_EMAIL);
    }

    // 닉네임 중복 체크
    @GetMapping("/check/nickname")
    public ApiResponse<?> checkNickname(@RequestParam String nickname) {
        validationService.validateNickname(nickname);
        return ApiResponse.success(ResponseCode.VALID_NICKNAME);
    }
}

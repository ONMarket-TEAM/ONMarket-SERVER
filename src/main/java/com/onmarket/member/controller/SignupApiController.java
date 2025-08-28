package com.onmarket.member.controller;

import com.onmarket.member.dto.SignupRequest;
import com.onmarket.member.service.impl.SignupServiceImpl;
import com.onmarket.response.ApiResponse;
import com.onmarket.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SignupApiController {

    private final SignupServiceImpl signupService;

    @PostMapping("/signup")
    public ApiResponse<?> signup(@RequestBody SignupRequest request) {
        signupService.signup(request);
        return ApiResponse.success(ResponseCode.SIGNUP_SUCCESS);
    }
}

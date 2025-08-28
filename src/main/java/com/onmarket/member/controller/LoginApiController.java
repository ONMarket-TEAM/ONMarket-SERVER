package com.onmarket.member.controller;

import com.onmarket.member.dto.LoginRequest;
import com.onmarket.member.dto.LoginResponse;
import com.onmarket.member.service.impl.LoginServiceImpl;
import com.onmarket.common.response.ApiResponse;
import com.onmarket.common.response.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth API", description = "인증 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginApiController {

    private final LoginServiceImpl loginService;

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자가 아이디와 비밀번호로 로그인합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호 불일치")
    })
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = loginService.login(request);
        return ApiResponse.success(ResponseCode.LOGIN_SUCCESS, response);
    }
}

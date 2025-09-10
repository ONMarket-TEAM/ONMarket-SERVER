package com.onmarket.member.controller;

import com.onmarket.member.dto.SignupRequest;
import com.onmarket.member.service.impl.SignupServiceImpl;
import com.onmarket.common.response.ApiResponse;
import com.onmarket.common.response.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth API", description = "회원가입/로그인 관련 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SignupApiController {

    private final SignupServiceImpl signupService;

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "409", description = "이미 존재하는 회원")
    })
    public ApiResponse<?> signup(@RequestBody SignupRequest request) {
        signupService.signup(request);
        return ApiResponse.success(ResponseCode.SIGNUP_SUCCESS);
    }
}

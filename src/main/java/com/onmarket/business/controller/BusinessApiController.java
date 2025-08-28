package com.onmarket.business.controller;

import com.onmarket.business.dto.BusinessRequest;
import com.onmarket.business.dto.BusinessResponse;
import com.onmarket.business.service.impl.BusinessService;
import com.onmarket.common.jwt.JwtTokenProvider;
import com.onmarket.member.exception.LoginException;
import com.onmarket.response.ApiResponse;
import com.onmarket.response.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
public class BusinessApiController {

    private final BusinessService businessService;
    private final JwtTokenProvider jwtTokenProvider;

    // JWT 토큰에서 이메일 추출
    private String extractEmailFromToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new LoginException(ResponseCode.AUTH_TOKEN_NOT_FOUND);
        }

        String token = header.substring(7);

        if (!jwtTokenProvider.validateToken(token)) {
            throw new LoginException(ResponseCode.AUTH_TOKEN_INVALID);
        }

        return jwtTokenProvider.getEmail(token);
    }

    // 사업장 등록
    @PostMapping
    public ApiResponse<BusinessResponse> registerBusiness(
            HttpServletRequest request,
            @Valid @RequestBody BusinessRequest businessRequest) {

        String email = extractEmailFromToken(request);
        BusinessResponse response = businessService.registerBusiness(email, businessRequest);

        return ApiResponse.success(ResponseCode.BUSINESS_REGISTER_SUCCESS, response);
    }

    // 회원의 사업장 조회
    @GetMapping
    public ApiResponse<List<BusinessResponse>> getMemberBusinesses(HttpServletRequest request) {
        String email = extractEmailFromToken(request);
        List<BusinessResponse> responses = businessService.getMemberBusinesses(email);

        return ApiResponse.success(ResponseCode.BUSINESS_READ_SUCCESS, responses);
    }
}

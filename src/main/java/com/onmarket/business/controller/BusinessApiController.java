package com.onmarket.business.controller;

import com.onmarket.business.dto.BusinessRequest;
import com.onmarket.business.dto.BusinessResponse;
import com.onmarket.business.service.impl.BusinessService;
import com.onmarket.common.jwt.JwtTokenProvider;
import com.onmarket.member.exception.LoginException;
import com.onmarket.response.ApiResponse;
import com.onmarket.response.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Business API", description = "사업장 관련 API")
@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
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
    @Operation(summary = "사업장 등록", description = "회원의 사업장을 등록합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "200", description = "사업장이 성공적으로 등록되었습니다."),
            @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "400", description = "요청 데이터가 잘못되었습니다."),
            @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "401", description = "인증 토큰이 유효하지 않습니다.")
    })
    public ApiResponse<BusinessResponse> registerBusiness(
            HttpServletRequest request,
            @Valid @RequestBody BusinessRequest businessRequest) {

        String email = extractEmailFromToken(request);
        BusinessResponse response = businessService.registerBusiness(email, businessRequest);

        return ApiResponse.success(ResponseCode.BUSINESS_REGISTER_SUCCESS, response);
    }

    // 회원의 사업장 조회
    @GetMapping
    @Operation(summary = "사업장 조회", description = "현재 로그인한 회원의 모든 사업장을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "200", description = "사업장 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "401", description = "인증 토큰이 유효하지 않습니다.")
    })
    public ApiResponse<List<BusinessResponse>> getMemberBusinesses(HttpServletRequest request) {
        String email = extractEmailFromToken(request);
        List<BusinessResponse> responses = businessService.getMemberBusinesses(email);

        return ApiResponse.success(ResponseCode.BUSINESS_READ_SUCCESS, responses);
    }
}

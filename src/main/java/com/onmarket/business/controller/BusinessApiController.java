package com.onmarket.business.controller;

import com.onmarket.business.dto.BusinessUpdateRequest;
import com.onmarket.business.dto.BusinessRequest;
import com.onmarket.business.dto.BusinessResponse;
import com.onmarket.business.service.BusinessService;
import com.onmarket.oauth.jwt.JwtTokenProvider;
import com.onmarket.member.exception.LoginException;
import com.onmarket.common.response.ApiResponse;
import com.onmarket.common.response.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    // 사업장 단건 조회
    @GetMapping("/{businessId}")
    @Operation(summary = "내 사업장 단건 조회", description = "businessId가 내 소유인지 검증 후 상세를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "내 소유 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사업장 없음")
    })
    public ApiResponse<BusinessResponse> getMyBusiness(
            @AuthenticationPrincipal String email,
            @PathVariable Long businessId
    ) {
        BusinessResponse body = businessService.getMyBusiness(email, businessId);
        return ApiResponse.success(ResponseCode.BUSINESS_READ_SUCCESS, body);
    }

    // 사업정보 수정
    @PatchMapping("/{businessId}")
    @Operation(
            summary = "사업장 정보 부분 수정",
            description = "업종/형태/지역(시/도명, 시군구명)/연매출/직원수/설립연도 중 전달된 값만 반영합니다."
    )

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 값"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "내 소유 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사업장 없음")
    })
    public ApiResponse<BusinessResponse> updateMyBusiness(
            @AuthenticationPrincipal String email,
            @PathVariable Long businessId,
            @RequestBody BusinessUpdateRequest request
    ) {
        BusinessResponse updated = businessService.updateMyBusiness(email, businessId, request);
        return ApiResponse.success(ResponseCode.BUSINESS_UPDATE_SUCCESS, updated);
    }

    // 사업장 삭제
    @DeleteMapping("/{businessId}")
    @Operation(summary = "내 사업장 삭제", description = "businessId가 내 소유인지 검증 후 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "내 소유 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사업장 없음")
    })
    public ApiResponse<Void> deleteMyBusiness(
            @AuthenticationPrincipal String email,
            @PathVariable Long businessId
    ) {
        businessService.deleteMyBusiness(email, businessId);
        return ApiResponse.success(ResponseCode.BUSINESS_DELETE_SUCCESS, null);
    }

    @PatchMapping("/{businessId}/main")
    @Operation(summary = "메인 사업장 변경", description = "사용자가 소유한 사업장 중 하나를 메인 사업장으로 변경합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "메인 사업장 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "내 소유 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사업장 없음")
    })
    public ApiResponse<Void> changeMainBusiness(
            @AuthenticationPrincipal String email,
            @PathVariable Long businessId
    ) {
        businessService.changeMainBusiness(email, businessId);
        return ApiResponse.success(ResponseCode.BUSINESS_UPDATE_SUCCESS, null);
    }


}

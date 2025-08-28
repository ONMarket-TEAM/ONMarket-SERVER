package com.onmarket.member.controller;

import com.onmarket.common.jwt.JwtTokenProvider;
import com.onmarket.member.domain.Member;
import com.onmarket.member.dto.LoginResponse;
import com.onmarket.member.dto.RefreshRequest;
import com.onmarket.member.exception.RefreshTokenException;
import com.onmarket.member.repository.MemberRepository;
import com.onmarket.response.ApiResponse;
import com.onmarket.response.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth API", description = "인증 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class RefreshApiController {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "Refresh Token을 이용해 새로운 Access Token과 Refresh Token을 발급합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
            @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "400", description = "Refresh Token이 유효하지 않음"),
            @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "401", description = "회원 정보를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "403", description = "저장된 Refresh Token과 일치하지 않음")
    })
    public ApiResponse<LoginResponse> refresh(@RequestBody RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        // 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RefreshTokenException(ResponseCode.INVALID_REFRESH_TOKEN);
        }

        // 토큰에서 이메일 추출
        String email = jwtTokenProvider.getEmail(refreshToken);
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RefreshTokenException(ResponseCode.MEMBER_NOT_FOUND));

        // 저장된 RefreshToken과 비교
        if (!refreshToken.equals(member.getRefreshToken())) {
            throw new RefreshTokenException(ResponseCode.REFRESH_TOKEN_MISMATCH);
        }

        // 새 토큰 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(email);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(email);

        // DB에 새로운 Refresh Token 저장
        member.updateRefreshToken(newRefreshToken);
        memberRepository.save(member);

        return ApiResponse.success(ResponseCode.TOKEN_REFRESH_SUCCESS,
                new LoginResponse(newAccessToken, newRefreshToken));
    }
}

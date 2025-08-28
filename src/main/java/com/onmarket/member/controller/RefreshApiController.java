package com.onmarket.member.controller;

import com.onmarket.common.jwt.JwtTokenProvider;
import com.onmarket.member.domain.Member;
import com.onmarket.member.dto.LoginResponse;
import com.onmarket.member.dto.RefreshRequest;
import com.onmarket.member.exception.RefreshTokenException;
import com.onmarket.member.repository.MemberRepository;
import com.onmarket.response.ApiResponse;
import com.onmarket.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RefreshApiController {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    @PostMapping("/refresh")
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
        member.setRefreshToken(newRefreshToken);
        memberRepository.save(member);

        return ApiResponse.success(ResponseCode.TOKEN_REFRESH_SUCCESS,
                new LoginResponse(newAccessToken, newRefreshToken));
    }
}

package com.onmarket.member.controller;

import com.onmarket.common.jwt.JwtTokenProvider;
import com.onmarket.member.domain.Member;
import com.onmarket.member.dto.LoginResponse;
import com.onmarket.member.dto.RefreshRequest;
import com.onmarket.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RefreshApiController {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        String email = jwtTokenProvider.getEmail(refreshToken);
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        if (!refreshToken.equals(member.getRefreshToken())) {
            throw new IllegalArgumentException("저장된 리프레시 토큰과 다릅니다.");
        }

        // 새 토큰들 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(email);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(email);

        // DB에 새로운 Refresh 토큰 저장
        member.setRefreshToken(newRefreshToken);
        memberRepository.save(member);

        return ResponseEntity.ok(new LoginResponse(newAccessToken, newRefreshToken));
    }
}

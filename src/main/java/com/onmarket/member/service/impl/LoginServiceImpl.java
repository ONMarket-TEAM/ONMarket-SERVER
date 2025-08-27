package com.onmarket.member.service.impl;

import com.onmarket.common.jwt.JwtTokenProvider;
import com.onmarket.member.domain.Member;
import com.onmarket.member.dto.LoginRequest;
import com.onmarket.member.dto.LoginResponse;
import com.onmarket.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginServiceImpl {

    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(member.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getEmail());

        // Refresh Token을 DB에 저장 (예: Member 테이블 컬럼 추가)
        member.setRefreshToken(refreshToken);
        memberRepository.save(member);

        return new LoginResponse(accessToken, refreshToken);
    }
}

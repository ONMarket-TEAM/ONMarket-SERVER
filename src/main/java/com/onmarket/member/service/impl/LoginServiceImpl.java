package com.onmarket.member.service.impl;

import com.onmarket.oauth.jwt.JwtTokenProvider;
import com.onmarket.member.domain.Member;
import com.onmarket.member.domain.enums.MemberStatus;
import com.onmarket.member.dto.LoginRequest;
import com.onmarket.member.dto.LoginResponse;
import com.onmarket.member.exception.LoginException;
import com.onmarket.member.repository.MemberRepository;
import com.onmarket.member.service.LoginService;
import com.onmarket.common.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new LoginException(ResponseCode.MEMBER_NOT_FOUND));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new LoginException(ResponseCode.INVALID_CREDENTIALS);
        }

        // 회원 상태 체크
        if (member.getStatus() == MemberStatus.DELETED) {
            throw new LoginException(ResponseCode.MEMBER_DELETED);
        }
        if (member.getStatus() == MemberStatus.INACTIVE) {
            throw new LoginException(ResponseCode.AUTHENTICATION_REQUIRED);
        }

        // JWT 토큰 생성
        String role = member.getRole().name();
        String newAccessToken = jwtTokenProvider.createAccessToken(member.getEmail(), role);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(member.getEmail(), role);

        // Refresh Token 저장
        member.updateRefreshToken(newRefreshToken);
        memberRepository.save(member);

        return new LoginResponse(newAccessToken, newRefreshToken);
    }

    @Override
    public void logout(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new LoginException(ResponseCode.AUTH_TOKEN_NOT_FOUND);
        }

        String jwt;
        try {
            jwt = accessToken.replace("Bearer ", "").trim();
        } catch (Exception e) {
            throw new LoginException(ResponseCode.AUTH_TOKEN_INVALID);
        }

        // 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(jwt)) {
            throw new LoginException(ResponseCode.AUTH_TOKEN_INVALID);
        }

        // 만료 여부 체크
        if (jwtTokenProvider.isTokenExpired(jwt)) {
            throw new LoginException(ResponseCode.AUTH_TOKEN_EXPIRED);
        }

        String email = jwtTokenProvider.getEmail(jwt);

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new LoginException(ResponseCode.MEMBER_NOT_FOUND));

        member.updateRefreshToken(null);

        try {
            memberRepository.save(member);
        } catch (Exception e) {
            throw new LoginException(ResponseCode.SERVER_ERROR);
        }
    }



}
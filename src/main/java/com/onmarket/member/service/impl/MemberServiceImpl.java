package com.onmarket.member.service.impl;

import com.onmarket.member.domain.Member;
import com.onmarket.member.exception.LoginException;
import com.onmarket.member.repository.MemberRepository;
import com.onmarket.business.exception.BusinessException;
import com.onmarket.common.response.ResponseCode;
import com.onmarket.member.service.MemberService;
import com.onmarket.oauth.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Member findByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));
    }

    @Override
    public Member findById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));
    }

    @Override
    public void withdraw(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new LoginException(ResponseCode.AUTH_TOKEN_NOT_FOUND);
        }

        String jwt = accessToken.replace("Bearer ", "");
        if (!jwtTokenProvider.validateToken(jwt)) {
            throw new LoginException(ResponseCode.AUTH_TOKEN_INVALID);
        }

        String email = jwtTokenProvider.getEmail(jwt);
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new LoginException(ResponseCode.MEMBER_NOT_FOUND));

        try {
            memberRepository.delete(member);
        } catch (Exception e) {
            throw new LoginException(ResponseCode.MEMBER_WITHDRAW_FAILED);
        }
    }
}

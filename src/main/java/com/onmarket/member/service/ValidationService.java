package com.onmarket.member.service.impl;

import com.onmarket.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValidationService {

    private final MemberRepository memberRepository;

    public boolean isEmailDuplicate(String email) {
        return memberRepository.existsByEmail(email);
    }

    public boolean isNicknameDuplicate(String nickname) {
        return memberRepository.existsByNickname(nickname);
    }
}

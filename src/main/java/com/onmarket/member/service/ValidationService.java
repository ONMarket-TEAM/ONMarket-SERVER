package com.onmarket.member.service.impl;

import com.onmarket.member.exception.ValidationException;
import com.onmarket.member.repository.MemberRepository;
import com.onmarket.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValidationService {

    private final MemberRepository memberRepository;

    public void validateEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new ValidationException(ResponseCode.DUPLICATED_EMAIL);
        }
    }

    public void validateNickname(String nickname) {
        if (memberRepository.existsByNickname(nickname)) {
            throw new ValidationException(ResponseCode.DUPLICATED_NICKNAME);
        }
    }
}

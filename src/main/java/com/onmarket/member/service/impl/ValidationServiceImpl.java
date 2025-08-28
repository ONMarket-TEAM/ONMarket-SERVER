package com.onmarket.member.service.impl;

import com.onmarket.member.exception.ValidationException;
import com.onmarket.member.repository.MemberRepository;
import com.onmarket.response.ResponseCode;
import com.onmarket.member.service.ValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValidationServiceImpl implements ValidationService {

    private final MemberRepository memberRepository;

    @Override
    public void validateEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new ValidationException(ResponseCode.DUPLICATED_EMAIL);
        }
    }

    @Override
    public void validateNickname(String nickname) {
        if (memberRepository.existsByNickname(nickname)) {
            throw new ValidationException(ResponseCode.DUPLICATED_NICKNAME);
        }
    }
}

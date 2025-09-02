package com.onmarket.member.service.impl;

import com.onmarket.common.response.ResponseCode;
import com.onmarket.member.domain.Member;
import com.onmarket.member.domain.enums.MemberStatus;
import com.onmarket.member.dto.SignupRequest;
import com.onmarket.member.exception.SignupException;
import com.onmarket.member.repository.MemberRepository;
import com.onmarket.member.service.SignupService;
import com.onmarket.member.service.ValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignupServiceImpl implements SignupService {

    private final MemberRepository memberRepository;
    private final ValidationService validationService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void signup(SignupRequest request) {
        // 이메일 중복 체크
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new SignupException(ResponseCode.DUPLICATED_EMAIL);
        }

        // 닉네임 중복 체크
        if (memberRepository.existsByNickname(request.getNickname())) {
            throw new SignupException(ResponseCode.DUPLICATED_NICKNAME);
        }

        // 비밀번호 유효성 검사
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new SignupException(ResponseCode.INVALID_PASSWORD_FORMAT);
        }

        // 회원 생성
        Member member = Member.builder()
                .username(request.getUsername()) // 동명이인 허용
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .email(request.getEmail())
                .phone(request.getPhone())
                .profileImage(request.getProfileImage())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .status(MemberStatus.ACTIVE)
                .build();

        memberRepository.save(member);
    }
}

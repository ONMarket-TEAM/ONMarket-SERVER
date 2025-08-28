package com.onmarket.member.service.impl;

import com.onmarket.member.domain.Member;
import com.onmarket.member.domain.enums.MemberStatus;
import com.onmarket.member.dto.SignupRequest;
import com.onmarket.member.exception.SignupException;
import com.onmarket.member.repository.MemberRepository;
import com.onmarket.member.service.SignupService;
import com.onmarket.member.service.ValidationService;
import com.onmarket.common.response.ResponseCode;
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
        // 유효성 검증
        validationService.validateEmail(request.getEmail());
        validationService.validateNickname(request.getNickname());

        // 아이디 중복 체크
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new SignupException(ResponseCode.DUPLICATED_EMAIL);
        }

        // 비밀번호 유효성 검사
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new SignupException(ResponseCode.INVALID_PASSWORD_FORMAT);
        }

        // 회원 생성
        Member member = Member.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .email(request.getEmail())
                .phone(request.getPhone())
                .profileImage(request.getProfileImage())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .mainBusinessId(request.getMainBusinessId())
                .status(MemberStatus.ACTIVE)
                .build();

        memberRepository.save(member);
    }
}

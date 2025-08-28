package com.onmarket.member.service.impl;

import com.onmarket.member.domain.Member;
import com.onmarket.member.domain.enums.MemberStatus;
import com.onmarket.member.dto.SignupRequest;
import com.onmarket.member.exception.SignupException;
import com.onmarket.member.repository.MemberRepository;
import com.onmarket.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignupServiceImpl {

    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public void signup(SignupRequest request) {
        // 아이디 중복 체크
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new SignupException(ResponseCode.DUPLICATED_EMAIL);
            // 혹은 ResponseCode.DUPLICATED_NICKNAME / 다른 적절한 코드
        }

        // 이메일 중복 체크
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new SignupException(ResponseCode.DUPLICATED_EMAIL);
        }

        // 닉네임 중복 체크
        if (memberRepository.existsByNickname(request.getNickname())) {
            throw new SignupException(ResponseCode.DUPLICATED_NICKNAME);
        }

        // 비밀번호 유효성 검사 (규칙 미준수 시 예외 발생)
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

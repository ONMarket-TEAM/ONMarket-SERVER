package com.onmarket.member.service.impl;

import com.onmarket.member.domain.Member;
import com.onmarket.member.domain.enums.MemberStatus;
import com.onmarket.member.dto.SignupRequest;
//import com.onmarket.member.exception.DuplicateUserException;
import com.onmarket.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignupServiceImpl {

    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public void signup(SignupRequest request) {
        if (memberRepository.existsByUsername(request.getUsername())) {
//            throw new DuplicateUserException("이미 존재하는 아이디입니다.");
        }

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

package com.onmarket.member.service.impl;

import com.onmarket.member.domain.Member;
import com.onmarket.member.dto.MemberUpdateRequest;
import com.onmarket.member.exception.LoginException;
import com.onmarket.member.repository.MemberRepository;
import com.onmarket.business.exception.BusinessException;
import com.onmarket.common.response.ResponseCode;
import com.onmarket.member.service.MemberService;
import com.onmarket.oauth.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.onmarket.member.exception.ValidationException;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

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

    private Member getMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));
    }

    /** 현재 비밀번호 일치 여부 검증 */
    @Override
    @Transactional(readOnly = true)
    public void verifyPassword(String email, String currentPassword) {
        Member m = findByEmail(email);

        // 소셜 계정인 경우 비밀번호 검증 불가
        if (m.getSocialProvider() != null) {
            throw new BusinessException(ResponseCode.SOCIAL_ACCOUNT_PASSWORD_VERIFICATION_NOT_ALLOWED);
        }

        if (!hasText(currentPassword)) {
            throw new ValidationException(ResponseCode.REQUIRED_CURRENT_PASSWORD);
        }
        // DB에는 암호화된 해시가 저장되어 있으므로 matches로 비교
        if (m.getPassword() == null || !passwordEncoder.matches(currentPassword, m.getPassword())) {
            throw new ValidationException(ResponseCode.PASSWORD_MISMATCH);
        }
    }

    /** 닉네임/비밀번호 변경 */
    @Override
    @Transactional
    public Member updateMember(String email, MemberUpdateRequest request) {
        Member m = findByEmail(email);

        // 1) 닉네임 변경
        if (request.getNickname() != null) {
            String nick = request.getNickname().trim();
            m.changeNickname(nick);
        }

        // 2) 비밀번호 변경
        boolean wantsPwChange = hasText(request.getNewPassword()) || hasText(request.getConfirmNewPassword());
        if (wantsPwChange) {
            // 소셜 계정인 경우 비밀번호 변경 불가
            if (m.getSocialProvider() != null) {
                throw new BusinessException(ResponseCode.SOCIAL_ACCOUNT_PASSWORD_CHANGE_NOT_ALLOWED);
            }
            if (!hasText(request.getCurrentPassword())) {
                throw new ValidationException(ResponseCode.REQUIRED_CURRENT_PASSWORD);
            }
            if (m.getPassword() == null || !passwordEncoder.matches(request.getCurrentPassword(), m.getPassword())) {
                throw new ValidationException(ResponseCode.PASSWORD_MISMATCH);
            }
            if (!Objects.equals(request.getNewPassword(), request.getConfirmNewPassword())) {
                throw new ValidationException(ResponseCode.NEW_PASSWORD_MISMATCH);
            }
            m.changePassword(passwordEncoder.encode(request.getNewPassword())); // 도메인 메서드 사용
        }

        // JPA dirty checking으로 자동 반영
        return m;
    }

    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}

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

            // 새 비밀번호 확인
            if (!hasText(request.getNewPassword()) || !hasText(request.getConfirmNewPassword())) {
                throw new ValidationException(ResponseCode.REQUIRED_NEW_PASSWORD);
            }

            if (!Objects.equals(request.getNewPassword(), request.getConfirmNewPassword())) {
                throw new ValidationException(ResponseCode.NEW_PASSWORD_MISMATCH);
            }

            // 비밀번호 복잡성 검증 (서버에서도 체크)
            validatePasswordStrength(request.getNewPassword());

            m.changePassword(passwordEncoder.encode(request.getNewPassword()));
        }

        // JPA dirty checking으로 자동 반영
        return m;
    }

    /**
     * 비밀번호 복잡성 검증
     */
    private void validatePasswordStrength(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new BusinessException(ResponseCode.PASSWORD_REQUIRED);
        }

        // 1. 길이 검증 (8~20자)
        if (password.length() < 8) {
            throw new BusinessException(ResponseCode.PASSWORD_TOO_SHORT);
        }
        if (password.length() > 20) {
            throw new BusinessException(ResponseCode.PASSWORD_TOO_LONG);
        }

        // 2. 복잡성 검증 - 최소 2가지 문자 조합 필요
        int complexity = 0;
        boolean hasLowercase = password.matches(".*[a-z].*");
        boolean hasUppercase = password.matches(".*[A-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

        if (hasLowercase) complexity++;
        if (hasUppercase) complexity++;
        if (hasDigit) complexity++;
        if (hasSpecialChar) complexity++;

        if (complexity < 2) {
            throw new BusinessException(ResponseCode.PASSWORD_COMPLEXITY_INSUFFICIENT);
        }

        // 3. 공백 문자 검증
        if (password.contains(" ") || password.contains("\t") || password.contains("\n")) {
            throw new BusinessException(ResponseCode.PASSWORD_WHITESPACE_NOT_ALLOWED);
        }
    }

    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /** 이름/휴대폰 기반으로 이메일 찾기 */
    @Override
    @Transactional(readOnly = true)
    public String findId(String username, String phone) {
        if (!hasText(username) || !hasText(phone)) {
            throw new ValidationException(ResponseCode.MISSING_REQUIRED_FIELDS);
        }

        return memberRepository.findByUsernameAndPhone(username, phone)
                .map(member -> {
                    if (member.getStatus().equals("DELETED")) {
                        throw new BusinessException(ResponseCode.MEMBER_DELETED);
                    }
                    return member.getEmail();
                })
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));

    }
}

package com.onmarket.member.service.impl;

import com.onmarket.member.domain.Member;
import com.onmarket.member.domain.enums.Gender;
import com.onmarket.member.domain.enums.MemberStatus;
import com.onmarket.member.dto.*;
import com.onmarket.member.exception.LoginException;
import com.onmarket.member.repository.MemberRepository;
import com.onmarket.business.exception.BusinessException;
import com.onmarket.common.response.ResponseCode;
import com.onmarket.member.service.MemberService;
import com.onmarket.notification.repository.NotificationSubscriptionRepository;
import com.onmarket.oauth.jwt.JwtTokenProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.onmarket.member.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final NotificationSubscriptionRepository notificationSubscriptionRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    @Override
    @Transactional(readOnly = true)
    public List<Member> findAllActiveMembers() {
        return memberRepository.findByStatus(MemberStatus.ACTIVE);
    }

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
            notificationSubscriptionRepository.deleteByMemberMemberId(member.getMemberId());
            memberRepository.delete(member);
        } catch (Exception e) {
            throw new LoginException(ResponseCode.MEMBER_WITHDRAW_FAILED);
        }
    }

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

        if (m.getPassword() == null || !passwordEncoder.matches(currentPassword, m.getPassword())) {
            throw new ValidationException(ResponseCode.PASSWORD_MISMATCH);
        }
    }

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

            if (passwordEncoder.matches(request.getNewPassword(), m.getPassword())) {
                throw new ValidationException(ResponseCode.SAME_AS_CURRENT_PASSWORD);
            }

            // 비밀번호 복잡성 검증 (서버에서도 체크)
            validatePasswordStrength(request.getNewPassword());

            m.changePassword(passwordEncoder.encode(request.getNewPassword()));
        }

        return m;
    }

    @Override
    @Transactional(readOnly = true)
    public String findId(String username, String phone) {
        if (!hasText(username) || !hasText(phone)) {
            throw new ValidationException(ResponseCode.MISSING_REQUIRED_FIELDS);
        }

        return memberRepository.findByUsernameAndPhone(username, phone)
                .map(member -> {
                    if (member.getStatus() == MemberStatus.DELETED) {
                        throw new BusinessException(ResponseCode.MEMBER_DELETED);
                    }
                    return member.getEmail();
                })
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));
    }

    @Override
    public Member save(Member member) {
        return memberRepository.save(member);
    }

    @Override
    @Transactional
    public SocialLoginResponse handleSocialLogin(SocialUserInfo info) {
        // 이메일 필수 체크
        if (info.getEmail() == null || info.getEmail().isBlank()) {
            throw new BusinessException(ResponseCode.OAUTH2_EMAIL_NOT_PROVIDED);
        }

        // 기존 회원 체크 (socialId + socialProvider로 찾기)
        Optional<Member> existingBySocial = memberRepository.findBySocialIdAndSocialProvider(
                info.getSocialId(), info.getSocialProvider());

        if (existingBySocial.isPresent()) {
            Member member = existingBySocial.get();

            if (member.isPending()) {
                // PENDING 상태면 닉네임 설정 필요
                return SocialLoginResponse.builder()
                        .memberId(member.getMemberId())
                        .email(member.getEmail())
                        .nickname(member.getNickname())
                        .status(MemberStatus.PENDING)
                        .build();
            } else {
                // ACTIVE 상태면 바로 로그인
                String accessToken = jwtTokenProvider.createAccessToken(member.getEmail(), member.getRole().name());
                String refreshToken = jwtTokenProvider.createRefreshToken(member.getEmail(), member.getRole().name());
                member.updateRefreshToken(refreshToken);

                return SocialLoginResponse.builder()
                        .memberId(member.getMemberId())
                        .email(member.getEmail())
                        .nickname(member.getNickname())
                        .profileImage(member.getProfileImage())
                        .status(MemberStatus.ACTIVE)
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();
            }
        }

        // 이메일로 기존 회원 체크 (다른 소셜 계정으로 가입했을 수도 있음)
        Optional<Member> existingByEmail = memberRepository.findByEmail(info.getEmail());

        // 신규 회원 → PENDING 상태로 생성
        Member pendingMember = Member.createSocialPendingMember(info);
        memberRepository.save(pendingMember);

        return SocialLoginResponse.builder()
                .memberId(pendingMember.getMemberId())
                .email(pendingMember.getEmail())
                .nickname(pendingMember.getNickname())
                .profileImage(pendingMember.getProfileImage())
                .status(MemberStatus.PENDING)
                .build();
    }

    @Override
    @Transactional
    public CompleteSocialSignupResponse completeSocialSignup(
            Long memberId,
            String nickname,
            String profileImageKey,
            String username,
            String phone,
            LocalDate birthDate,
            Gender gender
    ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));

        // 닉네임 중복 체크
        if (memberRepository.existsByNicknameAndMemberIdNot(nickname.trim(), member.getMemberId())) {
            throw new BusinessException(ResponseCode.DUPLICATED_NICKNAME);
        }

        // 이름 업데이트
        if (username != null && !username.isBlank()) {
            member.updateUsername(username.trim());
        }

        // 전화번호 업데이트
        if (phone != null && !phone.isBlank()) {
            member.updatePhone(phone.trim());
        }

        // 생년월일 업데이트
        if (birthDate != null) {
            member.updateBirthDate(birthDate);
        }

        // 성별 업데이트
        if (gender != null) {
            member.updateGender(gender);
        }

        // 닉네임 설정 + ACTIVE 상태로 변경
        member.completeSignup(nickname.trim());

        // 프로필 이미지 키 반영 (있을 때만)
        if (profileImageKey != null && !profileImageKey.isBlank()) {
            member.changeProfileImage(profileImageKey);
        }

        // 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(member.getEmail(), member.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getEmail(), member.getRole().name());
        member.updateRefreshToken(refreshToken);

        memberRepository.save(member);

        return CompleteSocialSignupResponse.builder()
                .memberId(member.getMemberId())
                .username(member.getUsername())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .phone(member.getPhone())
                .birthDate(member.getBirthDate())
                .gender(member.getGender())
                .profileImage(member.getProfileImage())
                .instagramUsername(member.getInstagramUsername())
                .status(MemberStatus.ACTIVE)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

   /**
     * 비밀번호 복잡성 검증
     * 영문, 숫자, 특수문자를 모두 포함하여 8~20자로 입력
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
        boolean hasLetter = hasLowercase || hasUppercase;

        if (hasLowercase) complexity++;
        if (hasUppercase) complexity++;
        if (hasDigit) complexity++;
        if (hasSpecialChar) complexity++;

        if (!hasLetter || !hasDigit || !hasSpecialChar) {
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
}
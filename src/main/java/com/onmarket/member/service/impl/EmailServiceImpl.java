package com.onmarket.member.service.impl;

import com.onmarket.common.response.ResponseCode;
import com.onmarket.business.exception.BusinessException;
import com.onmarket.member.domain.Member;
import com.onmarket.member.repository.MemberRepository;
import com.onmarket.member.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Objects;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Duration CODE_TTL = Duration.ofMinutes(3);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(10); // 추가
    private final JavaMailSender mailSender;
    private final StringRedisTemplate redis;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.mail.username}")
    private String from;

    /** 인증코드 생성 */
    private String createCode() {
        int code = new Random().nextInt(900_000) + 100_000;
        return String.valueOf(code);
    }

    /** HTML 메일 구성 */
    private MimeMessage createEmailForm(String email, String code) throws MessagingException {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");

        helper.setTo(email);
        helper.setFrom(from);
        helper.setSubject("[ONMarket] 이메일 인증번호");

        String html = """
                <div style="font-family: Arial, sans-serif; padding: 30px; background-color: #1e1e1e; color: #fff;">
                  <h1 style="font-size: 24px; color: white; margin:0;">이메일 인증번호 안내</h1>
                  <p style="font-size: 16px; margin-top: 16px; line-height: 1.6;">
                    아래의 <strong>이메일 인증번호</strong>를 입력하여 본인확인을 진행해주세요.
                  </p>
                  <div style="background-color: #2a2a2a; padding: 24px; margin-top: 24px; margin-bottom: 24px; border-radius: 8px; text-align: center;">
                    <span style="font-size: 32px; font-weight: bold; letter-spacing: 3px; color: white;">%s</span>
                  </div>
                  <p style="font-size: 13px; color: #ccc;">유효시간: 3분</p>
                  <p style="font-size: 14px; color: #ccc; margin-top: 8px;">감사합니다.<br>ONMarket 드림</p>
                </div>
                """.formatted(code);

        helper.setText(html, true);
        return msg;
    }

    @Override
    public void sendVerificationCode(String email) {

        if (!memberRepository.existsByEmail(email)) {
            throw new BusinessException(ResponseCode.MEMBER_NOT_FOUND);
        }

        // 기존 코드가 있으면 삭제 후 재발급
        if (Boolean.TRUE.equals(redis.hasKey(email))) {
            redis.delete(email);
        }

        String code = createCode();
        try {
            MimeMessage form = createEmailForm(email, code);
            mailSender.send(form);
        } catch (MessagingException e) {
            throw new BusinessException(ResponseCode.MAIL_SEND_FAIL);
        }

        // 인증코드 저장 (TTL 3분)
        redis.opsForValue().set(email, code, CODE_TTL);
    }

    @Override
    public void verifyEmailCode(String email, String code) {

        if (!memberRepository.existsByEmail(email)) {
            throw new BusinessException(ResponseCode.MEMBER_NOT_FOUND);
        }

        String saved = redis.opsForValue().get(email);
        if (saved == null) {
            throw new BusinessException(ResponseCode.INVALID_MAIL);
        }
        if (!saved.equals(code)) {
            throw new BusinessException(ResponseCode.INVALID_CODE);
        }

        redis.delete(email);
        redis.opsForValue().set(email + ":verified", "true", VERIFIED_TTL);
    }

    @Override
    @Transactional
    public void resetPassword(String email, String newPassword, String confirmNewPassword) {

        String verified = redis.opsForValue().get(email + ":verified");
        if(!"true".equals(verified)) {
            throw new BusinessException(ResponseCode.EMAIL_VERIFICATION_REQUIRED);
        }

        // 1. 회원 존재 여부 확인
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));

        // 2. 소셜 계정 확인
        if (member.getSocialProvider() != null) {
            throw new BusinessException(ResponseCode.SOCIAL_ACCOUNT_PASSWORD_CHANGE_NOT_ALLOWED);
        }

        // 3. 비밀번호 확인 일치 검증
        if (!Objects.equals(newPassword, confirmNewPassword)) {
            throw new BusinessException(ResponseCode.NEW_PASSWORD_MISMATCH);
        }

        // 4. 비밀번호 강도 검증
        validatePasswordStrength(newPassword);

        // 5. 비밀번호 변경
        member.changePassword(passwordEncoder.encode(newPassword));

        // 6. 인증 관련 Redis 데이터 정리
        redis.delete(email + ":verified");
    }

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

        // 2. 복잡성 검증 - 최소 3가지 문자 조합 필요
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

    @Override
    public void delete(String key) {
        redis.delete(key);
    }
}

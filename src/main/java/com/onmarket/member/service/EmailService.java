package com.onmarket.member.service;

public interface EmailService {

    /** 이메일 인증코드 발송 */
    void sendVerificationCode(String email);

    /** 이메일 인증 */
    void verifyEmailCode(String email, String code);

    /** 비밀번호 재설정 */
    void resetPassword(String email, String newPassword, String confirmNewPassword);

    /** Redis 키 삭제 */
    void delete(String key);
}

package com.onmarket.member.service;

public interface SmsService {
    // 인증번호 발송
    String sendVerifyCode(String phoneNumber);

    // 인증번호 확인
    boolean verifyCode(String phoneNumber, String code);
}

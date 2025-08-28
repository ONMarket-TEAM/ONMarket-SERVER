package com.onmarket.member.controller;

import com.onmarket.member.dto.SmsRequest;
import com.onmarket.member.dto.SmsVerifyRequest;
import com.onmarket.member.service.impl.SmsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SmsApiController {

    private final SmsServiceImpl smsService;

    // 인증번호 발송
    @PostMapping("/verify-code")
    public ResponseEntity<String> sendVerifyCode(@RequestBody SmsRequest request) {
        smsService.sendVerifyCode(request.getPhoneNumber());
        return ResponseEntity.ok("인증번호가 발송되었습니다.");
    }

    // 인증번호 확인
    @PostMapping("/confirm-code")
    public ResponseEntity<Boolean> confirmVerifyCode(@RequestBody SmsVerifyRequest request) {
        boolean isValid = smsService.verifyCode(request.getPhoneNumber(), request.getCode());
        return ResponseEntity.ok(isValid);
    }
}

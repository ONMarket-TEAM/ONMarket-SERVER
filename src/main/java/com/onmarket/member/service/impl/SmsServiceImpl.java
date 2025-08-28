package com.onmarket.member.service.impl;

import com.onmarket.member.exception.SmsSendFailException;
import com.onmarket.member.exception.SmsVerifyFailedException;
import com.onmarket.member.service.SmsService;
import com.onmarket.response.ResponseCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.exception.NurigoMessageNotReceivedException;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private final StringRedisTemplate redisTemplate;
    private DefaultMessageService messageService;

    @Value("${sms.api-key}")
    private String apiKey;

    @Value("${sms.api-secret}")
    private String apiSecret;

    @Value("${sms.from-number}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret, "https://api.solapi.com");
    }

    @Override
    public String sendVerifyCode(String phoneNumber) {
        String code = String.format("%06d", new Random().nextInt(999999));

        // Redis에 저장 (유효시간 5분)
        redisTemplate.opsForValue().set("SMS:" + phoneNumber, code, Duration.ofMinutes(5));

        // 문자 발송
        Message message = new Message();
        message.setFrom(fromNumber);
        message.setTo(phoneNumber);
        message.setText("[OnMarket] 인증번호는 " + code + " 입니다. (5분간 유효)");

        try {
            messageService.send(message);
        } catch (NurigoMessageNotReceivedException e) {
            throw new SmsSendFailException(ResponseCode.SMS_SEND_FAIL);
        } catch (Exception e) {
            throw new SmsSendFailException(ResponseCode.SMS_SEND_FAIL);
        }

        return code;
    }

    @Override
    public boolean verifyCode(String phoneNumber, String code) {
        String savedCode = redisTemplate.opsForValue().get("SMS:" + phoneNumber);

        if (savedCode == null) {
            throw new SmsVerifyFailedException(ResponseCode.SMS_VERIFY_FAILED);
        }

        if (!savedCode.equals(code)) {
            throw new SmsVerifyFailedException(ResponseCode.SMS_VERIFY_FAILED);
        }

        // 일치하면 Redis에서 즉시 삭제 (보안 강화)
        redisTemplate.delete("SMS:" + phoneNumber);
        return true;
    }
}

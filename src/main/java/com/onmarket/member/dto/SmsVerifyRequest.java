package com.onmarket.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmsVerifyRequest {
    private String phoneNumber;
    private String code;
}

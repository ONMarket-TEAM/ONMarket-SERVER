package com.onmarket.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmsRequest {

    @Schema(description = "인증번호를 받을 휴대폰 번호", example = "01012345678")
    private String phoneNumber;
}

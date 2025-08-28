package com.onmarket.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmsVerifyRequest {

    @Schema(description = "인증받을 휴대폰 번호", example = "01012345678")
    private String phoneNumber;

    @Schema(description = "수신한 인증번호 (6자리 숫자)", example = "123456")
    private String code;
}

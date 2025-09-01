package com.onmarket.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FindIdRequest {
    @Schema(description = "사용자 이름", example = "홍길동")
    private String username;
    @Schema(description = "폰 번호", example = "010-1234-5678")
    private String phone;
}

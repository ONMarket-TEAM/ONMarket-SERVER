package com.onmarket.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordVerifyRequest {
    private String currentPassword;
}

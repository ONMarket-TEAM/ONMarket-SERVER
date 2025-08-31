package com.onmarket.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FindIdRequest {
    private String username;
    private String phone;
}

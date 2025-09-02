package com.onmarket.member.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProfileImagePresignRequest {
    private String filename;
    private String contentType;
}

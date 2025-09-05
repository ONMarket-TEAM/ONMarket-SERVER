package com.onmarket.caption.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenerateFromS3Request {
    @NotBlank private String s3Key; // 1단계에서 생성된 키
    @NotBlank private String prompt; // 2단계 사용자 입력 문구
}
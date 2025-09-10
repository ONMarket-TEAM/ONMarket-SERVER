package com.onmarket.caption.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GenerateFromS3Request {
    @JsonProperty("s3_key")
    private String s3Key;

    private String prompt;
}
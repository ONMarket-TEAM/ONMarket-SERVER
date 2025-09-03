package com.onmarket.caption.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class OptionStyle {
    private String tone;            // "트렌디","감성","간결","재치","진지" 등 (프론트에서 선택)
    private String language;        // "ko","en","ko+en"
    private boolean withHashtags = true;
    private boolean withEmojis = false; // 강제 금지(서비스에서 다시 false 처리)
    @Min(1) @Max(10)
    private int variations = 5;     // 생성 개수
    private Integer maxChars;       // 최대 글자수(옵션)
}
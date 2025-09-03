package com.onmarket.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "openai")
public class OpenAIProperties {
    private String apiKey;
    private String chatModel = "gpt-4o-mini";   // 전역 ChatGPT 기본값
    private String imageModel = "gpt-image-1";  // 필요 시 DALL·E (선택적)
    private Boolean enabled = true;   // 기본값 true
    private Integer maxChars = 5000;  // 기본 최대 글자 수
}
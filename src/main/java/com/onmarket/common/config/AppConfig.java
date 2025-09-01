package com.onmarket.common.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // RestTemplate이 응답을 처리할 때 사용할 메시지 컨버터 리스트를 가져옵니다.
        // StringHttpMessageConverter를 맨 앞에 추가하여 모든 응답을 UTF-8로 먼저 해석하도록 강제합니다.
        restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

        return restTemplate;
    }
}
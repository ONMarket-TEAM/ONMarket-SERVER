package com.onmarket.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter @Setter
@Component
@ConfigurationProperties(prefix = "app.s3")
public class AppS3Props {
    private int presignExpSeconds = 300;   // 프리사인드 URL 만료(필요 시 사용)
    private String publicBaseUrl;          // CDN/정적 도메인 (없으면 S3 URL 사용)
    private String keyPrefix = "cardnews"; // 업로드 키 prefix
}
package com.onmarket.oauth.config;

import com.onmarket.oauth.config.CloudAwsProps;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@RequiredArgsConstructor
public class S3Config {

    private final CloudAwsProps props;

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        var ak = props.getCredentials().getAccessKey();
        var sk = props.getCredentials().getSecretKey();
        if (ak != null && !ak.isBlank() && sk != null && !sk.isBlank()) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(ak, sk));
        }
        // 키가 yml에 없으면 EC2 IAM Role/환경변수 등 기본 체인 사용
        return DefaultCredentialsProvider.create();
    }

    @Bean
    public S3Client s3Client(AwsCredentialsProvider cred) {
        return S3Client.builder()
                .region(Region.of(props.getRegion().getStatic_()))
                .credentialsProvider(cred)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(AwsCredentialsProvider cred) {
        return S3Presigner.builder()
                .region(Region.of(props.getRegion().getStatic_()))
                .credentialsProvider(cred)
                .build();
    }
}

// src/main/java/com/onmarket/cardnews/service/S3Uploader.java
package com.onmarket.cardnews.service;

import com.onmarket.common.config.AppS3Props;
import com.onmarket.common.config.CloudAwsProps;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3Uploader {
    private final S3Client s3;
    private final CloudAwsProps awsProps;
    private final AppS3Props appProps;
    private final S3Presigner presigner;    // ✅ Config에서 Bean 주입받음

    public record PutResult(String bucket, String key, String etag, String url) {}

    /** PNG 업로드 (ACL 없이) */
    public PutResult uploadPng(byte[] bytes, String customKey) {
        String bucket = awsProps.getS3().getBucket();
        String key = (customKey != null && !customKey.isBlank())
                ? customKey
                : appProps.getKeyPrefix() + "/cardnews-" + System.currentTimeMillis() + ".png";

        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("image/png")
                .build();

        PutObjectResponse resp = s3.putObject(req, RequestBody.fromBytes(bytes));

        // publicBaseUrl 있으면 CDN URL, 없으면 presigned URL
        String url = (isNotBlank(appProps.getPublicBaseUrl()))
                ? buildPublicUrl(appProps.getPublicBaseUrl(), key)
                : presignedGetUrl(bucket, key, Duration.ofMinutes(30));

        return new PutResult(bucket, key, resp.eTag(), url);
    }

    /** presigned GET URL */
    public String presignedGetUrl(String bucket, String key, Duration ttl) {
        var get = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        var pre = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(get)
                .build();
        return presigner.presignGetObject(pre).url().toString();
    }

    private static String buildPublicUrl(String base, String key) {
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/" + encodePathKeepSlash(key);
    }

    /** ✅ 슬래시는 유지하고, segment만 인코딩 */
    private static String encodePathKeepSlash(String key) {
        String[] parts = key.split("/");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append('/');
            sb.append(URLEncoder.encode(parts[i], StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return sb.toString();
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
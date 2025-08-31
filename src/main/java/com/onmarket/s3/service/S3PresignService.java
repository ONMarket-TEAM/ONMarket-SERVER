package com.onmarket.s3.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3PresignService {

    private final S3Presigner presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${app.s3.presign-exp-seconds:300}")
    private long expSec;

    public String buildKey(String dir, String filename) {
        String safe = filename.replaceAll("[\\s\"'`<>;:/\\\\]+", "-");
        return "%s/%s-%s".formatted(dir, UUID.randomUUID(), safe);
    }

    public URL generatePutUrl(String key, String contentType) {
        var put = PutObjectRequest.builder()
                .bucket(bucket).key(key).contentType(contentType).build();

        var pre = PutObjectPresignRequest.builder()
                .putObjectRequest(put)
                .signatureDuration(Duration.ofSeconds(expSec))
                .build();

        PresignedPutObjectRequest p = presigner.presignPutObject(pre);
        return p.url();
    }

    public URL generateGetUrl(String key) {
        var get = GetObjectRequest.builder().bucket(bucket).key(key).build();

        var pre = GetObjectPresignRequest.builder()
                .getObjectRequest(get)
                .signatureDuration(Duration.ofSeconds(expSec))
                .build();

        PresignedGetObjectRequest p = presigner.presignGetObject(pre);
        return p.url();
    }
}

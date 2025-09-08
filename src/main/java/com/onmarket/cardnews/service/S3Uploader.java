package com.onmarket.cardnews.service;

import com.onmarket.common.config.AppS3Props;
import com.onmarket.common.config.CloudAwsProps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Uploader {

    private final S3Client s3;
    private final S3Presigner presigner;     // ✅ S3Config에서 주입
    private final CloudAwsProps aws;
    private final AppS3Props s3props;

    public String uploadCardNews(byte[] png, String nameHint) {
        String bucket = aws.getS3().getBucket();
        String key = s3props.getKeyPrefix() + "/" + LocalDate.now() + "/" + nameHint + ".png";

        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("image/png")
                .build();

        s3.putObject(req, RequestBody.fromBytes(png));

        // DB에는 key만 저장
        return key;
    }

    /** presigned URL (프론트에서 요청할 때만 사용) */
    public String generatePresignedUrl(String key) {
        String bucket = aws.getS3().getBucket();

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(getObjectRequest)
                .build();

        return presigner.presignGetObject(presignRequest).url().toString();
    }
}

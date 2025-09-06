package com.onmarket.caption.service;

import com.onmarket.common.config.AppS3Props;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3TempStorageService {
    private final S3Client s3;
    private final S3Presigner presigner;
    private final AppS3Props props;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String uploadTemp(MultipartFile file) {
        try {
            String ext = ext(file.getOriginalFilename());
            String key = String.format("%s/%s/%s.%s", props.getKeyPrefix(), todayPath(), UUID.randomUUID(), ext);
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes()));
            return key;
        } catch (Exception e) {
            throw new RuntimeException("S3 임시 업로드 실패", e);
        }
    }

    public PresignedUrl presignPut(String filename, String contentType) {
        String ext = ext(filename);
        String key = String.format("%s/%s/%s.%s", props.getKeyPrefix(), todayPath(), UUID.randomUUID(), ext);
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket).key(key).contentType(contentType).build();
        URL url = presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(props.getPresignExpSeconds()))
                .putObjectRequest(put).build()).url();
        String publicUrl = StringUtils.hasText(props.getPublicBaseUrl())
                ? props.getPublicBaseUrl() + "/" + key : null;
        return new PresignedUrl(url.toString(), key, publicUrl);
    }

    public String presignGetUrl(String key, int seconds) {
        GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(key).build();
        URL url = presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(seconds))
                .getObjectRequest(get).build()).url();
        return url.toString();
    }

    public boolean delete(String key) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (S3Exception e) {
            return false; // 로그로만 남기고 흐름 계속
        }
    }

    private static String todayPath() {
        LocalDate d = LocalDate.now();
        return String.format("%04d/%02d/%02d", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }
    private static String ext(String filename) {
        if (filename == null) return "jpg";
        int i = filename.lastIndexOf('.');
        return (i > -1) ? filename.substring(i + 1).toLowerCase() : "jpg";
    }

    public record PresignedUrl(String url, String key, String publicUrl) {}
}
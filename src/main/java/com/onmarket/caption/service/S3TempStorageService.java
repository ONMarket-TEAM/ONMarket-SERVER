// src/main/java/com/onmarket/caption/service/S3TempStorageService.java
package com.onmarket.caption.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3TempStorageService {

    private final S3Client s3;            // ✅ 기존 S3Config에서 만든 빈 사용 (수정 불필요)
    private final S3Presigner presigner;  // ✅ 기존 S3Config에서 만든 빈 사용 (수정 불필요)

    // ✅ 기존 yml 키만 사용
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    // 예) app.s3.key-prefix: cardnews  → 내부에서 'cardnews/' 로 맞춤
    @Value("${app.s3.key-prefix:temp/captions}")
    private String keyPrefix;

    // 있으면 퍼블릭 URL 사용, 없으면 프리사인 GET 사용
    @Value("${app.s3.publicBaseUrl:}")
    private String publicBaseUrl;

    // presigned GET 만료(초)
    @Value("${app.s3.presign-exp-seconds:300}")
    private int presignExpSeconds;

    public record Uploaded(String key, String url) {}

    @PostConstruct
    void init() {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("cloud.aws.s3.bucket 이 비어 있습니다.");
        }
        if (keyPrefix == null || keyPrefix.isBlank()) keyPrefix = "temp/captions";
        if (!keyPrefix.endsWith("/")) keyPrefix = keyPrefix + "/";
        if (publicBaseUrl != null && publicBaseUrl.endsWith("/")) {
            publicBaseUrl = publicBaseUrl.substring(0, publicBaseUrl.length() - 1);
        }
        if (presignExpSeconds < 60) presignExpSeconds = 300; // 최소 60초
        log.info("[Caption] S3 bucket={}, keyPrefix={}, publicBaseUrl={}, presignExpSeconds={}",
                bucket, keyPrefix, publicBaseUrl, presignExpSeconds);
    }

    public Uploaded uploadTemp(MultipartFile file, String contentType) {
        try {
            String ext = guessExt(file.getOriginalFilename());
            String key = keyPrefix + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + ext;

            // 업로드: 객체는 기본 private (퍼블릭 베이스 URL을 쓰는 경우만 클라이언트에서 접근 가능)
            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .build();

            s3.putObject(put, RequestBody.fromBytes(file.getBytes()));

            String url;
            if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
                // 퍼블릭 정적 URL 조합 방식 (버킷이 퍼블릭/정적호스팅일 때)
                String encKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
                url = publicBaseUrl + "/" + encKey;
            } else {
                // presigned GET URL 발급 (버킷 private인 경우)
                GetObjectRequest getReq = GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build();
                GetObjectPresignRequest presign = GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofSeconds(presignExpSeconds))
                        .getObjectRequest(getReq)
                        .build();
                url = presigner.presignGetObject(presign).url().toString();
            }
            return new Uploaded(key, url);

        } catch (Exception e) {
            log.error("S3 업로드 실패", e);
            throw new RuntimeException("S3 업로드 실패");
        }
    }

    public List<Uploaded> uploadTempAll(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return List.of();
        if (files.size() > 10) throw new IllegalArgumentException("이미지는 최대 10장까지 가능합니다.");
        List<Uploaded> list = new ArrayList<>();
        for (MultipartFile f : files) list.add(uploadTemp(f, f.getContentType()));
        return list;
    }

    public boolean delete(String key) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (Exception e) {
            log.warn("S3 삭제 실패 key={}", key, e);
            return false;
        }
    }

    public boolean deleteAll(List<String> keys) {
        boolean ok = true;
        for (String k : keys) ok &= delete(k);
        return ok;
    }

    private String guessExt(String filename) {
        if (filename == null) return "";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return ".png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return ".jpg";
        if (lower.endsWith(".webp")) return ".webp";
        return "";
    }
}
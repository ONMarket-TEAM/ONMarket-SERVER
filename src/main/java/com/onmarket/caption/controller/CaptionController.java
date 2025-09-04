package com.onmarket.caption.controller;

import com.onmarket.caption.dto.*;
import com.onmarket.caption.service.CaptionService;
import com.onmarket.caption.service.S3TempStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Caption API", description = "임시 S3 업로드 → OpenAI 캡션 생성 → S3 즉시 삭제")
@RestController
@RequestMapping("/api/captions")
@RequiredArgsConstructor
public class CaptionController {

    private final CaptionService captionService;
    private final S3TempStorageService s3;

    /** 방식 A) 파일 직접 업로드 → AI → 즉시 삭제 (multipart) */
    @PostMapping(
            path = "/generate",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "파일 업로드 + 캡션 생성",
            description = "업로드된 이미지를 임시 S3에 저장 → OpenAI 호출 → 원본 즉시 삭제 후 결과 반환"
    )
    @ApiResponse(responseCode = "200", description = "생성 성공",
            content = @Content(schema = @Schema(implementation = CaptionGenerateResponse.class)))
    public CaptionGenerateResponse uploadAndGenerate(
            @RequestPart("file") MultipartFile file,
            @RequestPart("prompt") String prompt
    ) {
        return captionService.generateFromFileAndDelete(file, prompt);
    }

    /** 1단계: 프론트가 직접 PUT 업로드하려는 경우 presigned URL 발급 (json) */
    @PostMapping(
            path = "/presign",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "S3 프리사인 URL 발급",
            description = "이미지 업로드용 PUT presigned URL, key, (퍼블릭인 경우) publicUrl 반환"
    )
    @ApiResponse(responseCode = "200", description = "발급 성공",
            content = @Content(schema = @Schema(implementation = PresignResponse.class)))
    public PresignResponse presign(@Valid @RequestBody PresignRequest req) {
        var p = s3.presignPut(req.getFilename(), req.getContentType());
        return new PresignResponse(p.url(), p.key(), p.publicUrl());
    }

    /** 방식 B) s3Key + 프롬프트 → AI → 즉시 삭제 (json) */
    @PostMapping(
            path = "/generate-from-s3",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "S3 키로 캡션 생성",
            description = "프런트가 S3에 PUT 완료 후 s3Key와 prompt만 전달하면 OpenAI 호출 후 원본 즉시 삭제"
    )
    @ApiResponse(responseCode = "200", description = "생성 성공",
            content = @Content(schema = @Schema(implementation = CaptionGenerateResponse.class)))
    public CaptionGenerateResponse generateFromS3(@Valid @RequestBody GenerateFromS3Request body) {
        return captionService.generateFromS3AndDelete(body.getS3Key(), body.getPrompt());
    }
}

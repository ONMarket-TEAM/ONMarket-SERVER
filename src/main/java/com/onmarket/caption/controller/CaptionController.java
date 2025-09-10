package com.onmarket.caption.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onmarket.caption.dto.*;
import com.onmarket.caption.service.CaptionService;
import com.onmarket.caption.service.S3TempStorageService;
import com.onmarket.common.response.ApiResponse;
import com.onmarket.common.response.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "캡션 생성 성공",
                    content = @Content(schema = @Schema(implementation = CaptionGenerateResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류 발생")
    })
    public ApiResponse<CaptionGenerateResponse> uploadAndGenerate(
            @RequestPart("file") MultipartFile file,
            @RequestPart("prompt") String prompt
    ) {
        try {
            CaptionGenerateResponse result = captionService.generateFromFileAndDelete(file, prompt);
            return ApiResponse.success(ResponseCode.S3_OPERATION_SUCCESS, result);
        } catch (Exception e) {
            return ApiResponse.fail(ResponseCode.S3_OPERATION_FAILED, e.getMessage());
        }
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프리사인 URL 발급 성공",
                    content = @Content(schema = @Schema(implementation = PresignResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "필수 파라미터 누락"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "S3 작업 처리 중 오류 발생")
    })
    public ApiResponse<PresignResponse> presign(@Valid @RequestBody PresignRequest req) {
        try {
            var p = s3.presignPut(req.getFilename(), req.getContentType());
            PresignResponse response = new PresignResponse(p.url(), p.key(), p.publicUrl());
            return ApiResponse.success(ResponseCode.S3_PRESIGN_PUT_SUCCESS, response);
        } catch (Exception e) {
            return ApiResponse.fail(ResponseCode.S3_OPERATION_FAILED, e.getMessage());
        }
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "캡션 생성 성공",
                    content = @Content(schema = @Schema(implementation = CaptionGenerateResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 바디 또는 필수 필드 누락"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류 발생")
    })
    public ApiResponse<CaptionGenerateResponse> generateFromS3(HttpServletRequest request) {
        try {
            // 요청 바디를 직접 읽기
            StringBuilder sb = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            String rawJson = sb.toString();
            System.out.println("읽은 원본 JSON: " + rawJson);

            if (rawJson == null || rawJson.trim().isEmpty()) {
                return ApiResponse.fail(ResponseCode.S3_MISSING_PARAMS, "요청 바디가 비어있습니다");
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(rawJson);

            // JSON 필드 존재 여부 확인
            if (!jsonNode.has("s3Key") || !jsonNode.has("prompt")) {
                return ApiResponse.fail(ResponseCode.S3_MISSING_PARAMS, "s3Key와 prompt 필드가 필요합니다");
            }

            String s3Key = jsonNode.get("s3Key").asText();
            String prompt = jsonNode.get("prompt").asText();

            System.out.println("파싱 성공 - s3Key: " + s3Key + ", prompt: " + prompt);

            // 값 검증
            if (s3Key == null || s3Key.trim().isEmpty()) {
                return ApiResponse.fail(ResponseCode.S3_KEY_REQUIRED, "s3Key가 비어있습니다");
            }
            if (prompt == null || prompt.trim().isEmpty()) {
                return ApiResponse.fail(ResponseCode.S3_MISSING_PARAMS, "prompt가 비어있습니다");
            }

            CaptionGenerateResponse result = captionService.generateFromS3AndDelete(s3Key, prompt);
            return ApiResponse.success(ResponseCode.S3_OPERATION_SUCCESS, result);
        } catch (Exception e) {
            System.out.println("예외 발생: " + e.getMessage());
            return ApiResponse.fail(ResponseCode.S3_OPERATION_FAILED, "처리 실패: " + e.getMessage());
        }
    }

    /** 방식 C) 다중 s3Keys + 프롬프트 → AI → 즉시 삭제 (json) - 새로 추가 */
    @PostMapping(
            path = "/generate-from-multiple-s3",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "다중 S3 키로 캡션 생성",
            description = "프런트가 여러 이미지를 S3에 PUT 완료 후 s3Keys 배열과 prompt를 전달하면 첫 3장을 분석하여 OpenAI 호출 후 원본들을 즉시 삭제"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "캡션 생성 성공",
                    content = @Content(schema = @Schema(implementation = CaptionGenerateResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 바디 또는 필수 필드 누락"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류 발생")
    })
    public ApiResponse<CaptionGenerateResponse> generateFromMultipleS3(HttpServletRequest request) {
        try {
            // 요청 바디를 직접 읽기
            StringBuilder sb = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            String rawJson = sb.toString();
            System.out.println("읽은 원본 JSON (다중): " + rawJson);

            if (rawJson == null || rawJson.trim().isEmpty()) {
                return ApiResponse.fail(ResponseCode.S3_MISSING_PARAMS, "요청 바디가 비어있습니다");
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(rawJson);

            // JSON 필드 존재 여부 확인
            if (!jsonNode.has("s3Keys") || !jsonNode.has("prompt")) {
                return ApiResponse.fail(ResponseCode.S3_MISSING_PARAMS, "s3Keys와 prompt 필드가 필요합니다");
            }

            JsonNode s3KeysNode = jsonNode.get("s3Keys");
            String prompt = jsonNode.get("prompt").asText();

            // s3Keys 배열 검증
            if (!s3KeysNode.isArray() || s3KeysNode.size() == 0) {
                return ApiResponse.fail(ResponseCode.S3_MISSING_PARAMS, "s3Keys는 비어있지 않은 배열이어야 합니다");
            }

            // s3Keys를 List<String>으로 변환
            List<String> s3Keys = new ArrayList<>();
            for (JsonNode keyNode : s3KeysNode) {
                String key = keyNode.asText();
                if (key != null && !key.trim().isEmpty()) {
                    s3Keys.add(key);
                }
            }

            if (s3Keys.isEmpty()) {
                return ApiResponse.fail(ResponseCode.S3_KEY_REQUIRED, "유효한 s3Key가 없습니다");
            }

            if (prompt == null || prompt.trim().isEmpty()) {
                return ApiResponse.fail(ResponseCode.S3_MISSING_PARAMS, "prompt가 비어있습니다");
            }

            System.out.println("파싱 성공 - s3Keys: " + s3Keys + ", prompt: " + prompt);

            CaptionGenerateResponse result = captionService.generateFromMultipleS3AndDelete(s3Keys, prompt);
            return ApiResponse.success(ResponseCode.S3_OPERATION_SUCCESS, result);
        } catch (Exception e) {
            System.out.println("예외 발생: " + e.getMessage());
            return ApiResponse.fail(ResponseCode.S3_OPERATION_FAILED, "처리 실패: " + e.getMessage());
        }
    }

    // 이미지 보기용(다운로드) presigned URL 발급
    @GetMapping(path = "/presign-view", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "S3 키로 미리보기 URL 발급", description = "버킷이 private일 때도 유효시간 내에 이미지를 <img src>로 표시 가능")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "미리보기 URL 발급 성공",
                    content = @Content(schema = @Schema(implementation = PresignResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "필수 파라미터 key 누락"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "S3 작업 처리 중 오류 발생")
    })
    public ApiResponse<PresignResponse> presignView(
            @RequestParam("key") String key,
            @RequestParam(name = "ttl", defaultValue = "180") int ttlSec
    ) {
        try {
            if (key == null || key.trim().isEmpty()) {
                return ApiResponse.fail(ResponseCode.S3_KEY_REQUIRED, "key 파라미터가 필요합니다");
            }

            // GET presigned URL 생성
            String viewUrl = s3.presignGetUrl(key, ttlSec);
            // PresignResponse 재활용: publicUrl은 private 버킷이면 null
            PresignResponse response = new PresignResponse(viewUrl, key, null);
            return ApiResponse.success(ResponseCode.S3_PRESIGN_GET_SUCCESS, response);
        } catch (Exception e) {
            return ApiResponse.fail(ResponseCode.S3_OPERATION_FAILED, e.getMessage());
        }
    }

    // S3에 있는 파일을 직접 삭제하는 API
    @DeleteMapping("/delete")
    @Operation(summary = "S3 파일 삭제", description = "주어진 S3 키에 해당하는 파일을 즉시 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "파일 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "필수 파라미터 key 누락"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "S3 작업 처리 중 오류 발생")
    })
    public ApiResponse<Void> deleteFile(@RequestParam("key") String key) {
        try {
            if (key == null || key.trim().isEmpty()) {
                return ApiResponse.fail(ResponseCode.S3_KEY_REQUIRED, "key 파라미터가 필요합니다");
            }

            s3.delete(key);
            return ApiResponse.success(ResponseCode.S3_DELETE_SUCCESS, null);
        } catch (Exception e) {
            return ApiResponse.fail(ResponseCode.S3_OPERATION_FAILED, e.getMessage());
        }
    }
}

package com.onmarket.s3.controller;

import com.onmarket.common.response.ApiResponse;
import com.onmarket.common.response.ResponseCode;
import com.onmarket.s3.dto.PresignGetResponse;
import com.onmarket.s3.dto.PresignPutResponse;
import com.onmarket.s3.service.S3PresignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
// ⚠️ import로 가져오지 마세요: io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URL;

@Tag(
        name = "S3 Presign API",
        description = "프리사인드 URL을 발급해 클라이언트가 S3에 직접 업/다운로드 하도록 합니다."
)
@RestController
@RequestMapping("/api/s3")
@RequiredArgsConstructor
public class S3Controller {

    private final S3PresignService svc;

    @Operation(
            summary = "업로드용 프리사인 URL 발급",
            description = "dir/filename/contentType을 받아 S3 PUT용 presigned URL을 발급합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = PresignPutResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "파라미터 오류", content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500", description = "서버 오류", content = @Content
            )
    })
    @PostMapping(value = "/presign-put", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ApiResponse<PresignPutResponse> presignPut(
            @Parameter(description = "저장 프리픽스. 예) uploads, user-uploads/{userId}", example = "uploads")
            @RequestParam String dir,
            @Parameter(description = "원본 파일명. 서버가 UUID를 붙여 최종 key를 생성", example = "test.png")
            @RequestParam String filename,
            @Parameter(description = "업로드 MIME 타입 (PUT의 Content-Type과 동일)", example = "image/png")
            @RequestParam String contentType
    ) {
        if (!StringUtils.hasText(dir) || !StringUtils.hasText(filename) || !StringUtils.hasText(contentType)) {
            // ❗ ApiResponse.error 사용하지 않고 success(코드)로 반환
            return ApiResponse.success(ResponseCode.S3_MISSING_PARAMS);
        }
        try {
            String key = svc.buildKey(dir, filename);
            URL url = svc.generatePutUrl(key, contentType);
            return ApiResponse.success(
                    ResponseCode.S3_PRESIGN_PUT_SUCCESS,
                    new PresignPutResponse(url.toString(), key, contentType)
            );
        } catch (Exception e) {
            return ApiResponse.success(ResponseCode.S3_OPERATION_FAILED);
        }
    }

    @Operation(
            summary = "다운로드용 프리사인 URL 발급",
            description = "저장된 객체 key로 제한 시간 동안 유효한 다운로드 URL을 발급합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = PresignGetResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "파라미터 오류", content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500", description = "서버 오류", content = @Content
            )
    })
    @GetMapping("/presign-get")
    public ApiResponse<PresignGetResponse> presignGet(
            @Parameter(description = "S3 객체 key(업로드 시 받은 값)", example = "uploads/uuid-test.png")
            @RequestParam String key
    ) {
        if (!StringUtils.hasText(key)) {
            return ApiResponse.success(ResponseCode.S3_KEY_REQUIRED);
        }
        try {
            URL url = svc.generateGetUrl(key);
            return ApiResponse.success(
                    ResponseCode.S3_PRESIGN_GET_SUCCESS,
                    new PresignGetResponse(url.toString())
            );
        } catch (Exception e) {
            return ApiResponse.success(ResponseCode.S3_OPERATION_FAILED);
        }
    }
}
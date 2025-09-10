package com.onmarket.cardnews.controller;

import com.onmarket.cardnews.dto.TargetType;
import com.onmarket.cardnews.service.CardNewsService;
import com.onmarket.common.response.ApiResponse;
import com.onmarket.common.response.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@Tag(name = "Card News API", description = "카드뉴스 자동 생성 및 이미지 프록시 API")
@RestController
@RequestMapping("/api/cardnews")
@RequiredArgsConstructor
@Slf4j
public class CardNewsController {

    private final CardNewsService service;

    /** 카드뉴스 자동 생성 + 업로드 후 프록시 URL 반환 */
    @PostMapping("/auto")
    @Operation(summary = "카드뉴스 자동 생성", description = "타입/ID로 카드뉴스를 생성하고 S3에 업로드한 뒤 프록시 URL을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "카드뉴스 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 파라미터 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ApiResponse<Map<String, Object>> auto(@RequestParam TargetType type,
                                                 @RequestParam String id) {
        try {
            if (type == null || id == null || id.isBlank()) {
                return ApiResponse.fail(ResponseCode.CARDNEWS_INVALID_PARAMS, "type과 id는 필수입니다.");
            }
            String url = service.buildFromDbAndPersist(type, id);
            // 필요 시 key를 함께 내려주고 싶다면 service에서 반환하도록 확장하세요.
            return ApiResponse.success(
                    ResponseCode.CARDNEWS_BUILD_SUCCESS,
                    Map.of("url", url, "type", type.name(), "id", id)
            );
        } catch (Exception e) {
            log.error("카드뉴스 자동 생성 실패 - type: {}, id: {}", type, id, e);
            return ApiResponse.fail(ResponseCode.CARDNEWS_BUILD_FAILED, e.getMessage());
        }
    }

    /** presigned URL로 Redirect (이미지 직접 제공용) */
    @GetMapping("/image")
    @Operation(summary = "카드뉴스 이미지 프록시", description = "S3 프리사인 URL로 302 리다이렉트합니다.")
    public ResponseEntity<Void> image(@RequestParam String key) {
        try {
            if (key == null || key.isBlank()) {
                return ResponseEntity.badRequest().build(); // S3_KEY_REQUIRED 사용 대신 400 처리
            }
            String url = service.getPresignedUrlForKey(key);
            return ResponseEntity.status(HttpStatus.FOUND) // 302 Redirect
                    .location(URI.create(url))
                    .build();
        } catch (Exception e) {
            log.error("카드뉴스 이미지 리다이렉트 실패 - key: {}", key, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
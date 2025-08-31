package com.onmarket.supportsdata.controller;

import com.onmarket.response.ApiResponse;
import com.onmarket.response.ResponseCode;
import com.onmarket.supportsdata.service.PublicDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import java.util.Map;
import java.util.HashMap;

@Tag(name = "Support API", description = "정부 지원금 API")
@RestController
@RequestMapping("/api/support-products")
@RequiredArgsConstructor
public class PublicDataController {

    private final PublicDataService publicDataService;

    @PostMapping("/fetch-all")
    @Operation(summary = "전체 지원금데이터 수집", description = "모든 정부지원금 상품을 가져오는 비동기 작업을 시작시킵니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "데이터 수집 작업이 성공적으로 시작되었습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "데이터 수집 작업 시작 중 오류가 발생했습니다.")
    })
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> fetchAndSaveAllData() {
        return publicDataService.findAndSaveAllServices()
                .then(Mono.fromCallable(() -> {
                    // 성공 시 응답 데이터 구성
                    Map<String, Object> data = new HashMap<>();
                    data.put("message", "비동기 데이터 수집 및 저장 작업이 시작되었습니다.");
                    data.put("timestamp", System.currentTimeMillis());

                    ApiResponse<Map<String, Object>> successResponse = ApiResponse.success(ResponseCode.DATA_FETCH_SUCCESS, data);

                    return ResponseEntity.ok(successResponse);
                }))
                .onErrorResume(e -> {
                    ApiResponse<Map<String, Object>> errorResponse = ApiResponse.fail(ResponseCode.DATA_FETCH_FAILURE, e.getMessage());

                    return Mono.just(ResponseEntity
                            .status(ResponseCode.DATA_FETCH_FAILURE.getHttpStatus())
                            .body(errorResponse));
                });
    }
}
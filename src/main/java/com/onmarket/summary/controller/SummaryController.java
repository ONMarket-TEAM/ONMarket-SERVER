package com.onmarket.summary.controller;

import com.onmarket.common.response.ApiResponse;
import com.onmarket.common.response.ResponseCode;
import com.onmarket.summary.service.SummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Summary API", description = "상품 요약(short/long) 자동 생성 및 백필 API")
@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
@Slf4j
public class SummaryController {

    private final SummaryService service;

    /* -------------------- 단건 생성 -------------------- */

    @PostMapping("/loan/{id}")
    @Operation(summary = "Loan 단건 요약 생성", description = "loan_product 해당 ID의 요약(short/long)을 생성/저장합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요약 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 파라미터 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ApiResponse<String> loan(@PathVariable long id) {
        try {
            service.generateForLoan(id);
            return ApiResponse.success(ResponseCode.SUMMARY_GENERATE_SUCCESS, "loan_id=" + id);
        } catch (Exception e) {
            log.error("Loan 요약 생성 실패 - id: {}", id, e);
            return ApiResponse.fail(ResponseCode.SUMMARY_GENERATE_FAILED, e.getMessage());
        }
    }

    @PostMapping("/credit/{id}")
    @Operation(summary = "CreditLoan 단건 요약 생성", description = "credit_loan_product 해당 ID의 요약(short/long)을 생성/저장합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요약 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 파라미터 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ApiResponse<String> credit(@PathVariable long id) {
        try {
            service.generateForCredit(id);
            return ApiResponse.success(ResponseCode.SUMMARY_GENERATE_SUCCESS, "credit_id=" + id);
        } catch (Exception e) {
            log.error("Credit 요약 생성 실패 - id: {}", id, e);
            return ApiResponse.fail(ResponseCode.SUMMARY_GENERATE_FAILED, e.getMessage());
        }
    }

    @PostMapping("/support/{serviceId}")
    @Operation(summary = "Support 단건 요약 생성", description = "supportservice/support_product 해당 ID의 요약(short/long)을 생성/저장합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요약 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 파라미터 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ApiResponse<String> support(@PathVariable long serviceId) {
        try {
            service.generateForSupport(serviceId);
            return ApiResponse.success(ResponseCode.SUMMARY_GENERATE_SUCCESS, "support_id=" + serviceId);
        } catch (Exception e) {
            log.error("Support 요약 생성 실패 - serviceId: {}", serviceId, e);
            return ApiResponse.fail(ResponseCode.SUMMARY_GENERATE_FAILED, e.getMessage());
        }
    }
}
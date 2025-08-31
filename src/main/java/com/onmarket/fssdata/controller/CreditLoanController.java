package com.onmarket.fssdata.controller;

import com.onmarket.fssdata.domain.CreditLoanOption;
import com.onmarket.fssdata.domain.CreditLoanProduct;
import com.onmarket.fssdata.service.CreditLoanService;
import com.onmarket.response.ApiResponse;
import com.onmarket.response.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "CreditLoan API", description = "금융감독원 개인신용대출 API")
@RestController
@RequestMapping("/api/credit-loans")
@RequiredArgsConstructor
@Slf4j
public class CreditLoanController {

    private final CreditLoanService creditLoanService;

    // 데이터 수집 API - 모든 권역
    @PostMapping("/fetch-all")
    @Operation(summary = "전체 권역 데이터 수집", description = "모든 개인신용대출 상품을 외부 API를 통해 가져와 DB에 저장합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "데이터 수집 및 저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "외부 API 호출 또는 데이터베이스 저장 중 서버 내부 오류 발생")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> fetchData() {
        try {
            log.info("전체 권역 데이터 수집 시작");
            creditLoanService.fetchAndSaveAllCreditLoanData();

            Map<String, Object> data = new HashMap<>();
            data.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.DATA_FETCH_SUCCESS, data));
        } catch (Exception e) {
            log.error("데이터 수집 실패: ", e);
            return ResponseEntity
                    .status(ResponseCode.DATA_FETCH_FAILURE.getHttpStatus())
                    .body(ApiResponse.fail(ResponseCode.DATA_FETCH_FAILURE, e.getMessage()));
        }
    }

    // 특정 권역 데이터 수집
    @PostMapping("/fetch/{topFinGrpNo}")
    @Operation(summary = "특정 권역 데이터 수집", description = "지정한 금융권역의 개인신용대출 상품 데이터를 가져와 DB에 저장합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "데이터 수집 및 저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "존재하지 않는 금융권역 코드(topFinGrpNo) 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "외부 API 호출 또는 데이터베이스 저장 중 서버 내부 오류 발생")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> fetchDataByTopFinGrp(@PathVariable String topFinGrpNo) {
        String sectorName = getSectorName(topFinGrpNo);
        if ("알 수 없는 권역".equals(sectorName)) {
            return ResponseEntity
                    .status(ResponseCode.DATA_FETCH_FAILURE.getHttpStatus())
                    .body(ApiResponse.fail(ResponseCode.DATA_FETCH_FAILURE, "유효하지 않은 금융권역 코드입니다."));
        }

        try {
            log.info("권역 {} ({}) 데이터 수집 시작", topFinGrpNo, sectorName);
            creditLoanService.fetchAndSaveCreditLoanData(topFinGrpNo);

            Map<String, Object> data = new HashMap<>();
            data.put("topFinGrpNo", topFinGrpNo);
            data.put("sectorName", sectorName);

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.DATA_FETCH_SUCCESS, data));
        } catch (Exception e) {
            log.error("권역 {} 데이터 수집 실패: ", topFinGrpNo, e);
            return ResponseEntity
                    .status(ResponseCode.SERVER_ERROR.getHttpStatus()) // 500 에러로 명시
                    .body(ApiResponse.fail(ResponseCode.DATA_FETCH_FAILURE, e.getMessage()));
        }
    }

    // 데이터 수집 상태 확인
    @GetMapping("/status")
    @Operation(summary = "데이터 수집 상태 확인", description = "DB에 저장된 상품 및 옵션의 총 개수를 확인합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "데이터 상태 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "데이터베이스 조회 중 서버 내부 오류 발생")
    })
    public ApiResponse<Map<String, Object>> getDataStatus() {
        Map<String, Object> status = new HashMap<>();
        long totalProducts = creditLoanService.getTotalProductCount();
        long totalOptions = creditLoanService.getTotalOptionCount();

        status.put("totalProducts", totalProducts);
        status.put("totalOptions", totalOptions);
        status.put("isEmpty", totalProducts == 0);

        return ApiResponse.success(ResponseCode.DATA_STATUS_READ_SUCCESS, status);
    }

    // 사용자용 API

    // 금융회사별 상품 조회
    @GetMapping("/products")
    @Operation(summary = "금융회사별 상품 조회", description = "특정 금융회사의 상품 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공. 결과가 없을 경우 빈 배열([])을 반환합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "데이터베이스 조회 중 서버 내부 오류 발생")
    })
    public ApiResponse<List<CreditLoanProduct>> getProductsByCompany(@RequestParam String companyName) {
        List<CreditLoanProduct> products = creditLoanService.getProductsByCompany(companyName);
        return ApiResponse.success(ResponseCode.PRODUCT_READ_SUCCESS, products);
    }

    // 상품별 금리 옵션 조회
    @GetMapping("/options/{finPrdtCd}")
    @Operation(summary = "상품별 금리 옵션 조회", description = "특정 상품의 금리 옵션 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공. 결과가 없을 경우 빈 배열([])을 반환합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "데이터베이스 조회 중 서버 내부 오류 발생")
    })
    public ApiResponse<List<CreditLoanOption>> getOptionsByProduct(@PathVariable String finPrdtCd) {
        List<CreditLoanOption> options = creditLoanService.getOptionsByProduct(finPrdtCd);
        return ApiResponse.success(ResponseCode.OPTION_READ_SUCCESS, options);
    }

    // 모든 상품 조회 (페이징)
    @GetMapping("/products/all")
    @Operation(summary = "모든 상품 페이징 조회", description = "모든 상품을 페이지 단위로 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공. 결과가 없을 경우 빈 배열([])을 반환합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "데이터베이스 조회 중 서버 내부 오류 발생")
    })
    public ApiResponse<List<CreditLoanProduct>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<CreditLoanProduct> products = creditLoanService.getAllProducts(page, size);
        return ApiResponse.success(ResponseCode.PRODUCT_READ_SUCCESS, products);
    }


    private String getSectorName(String topFinGrpNo) {
        switch (topFinGrpNo) {
            case "020000": return "은행";
            case "030200": return "여신전문금융회사";
            case "030300": return "저축은행";
            case "050000": return "보험회사";
            case "060000": return "금융투자회사";
            default: return "알 수 없는 권역";
        }
    }
}
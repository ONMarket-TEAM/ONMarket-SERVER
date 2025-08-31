package com.onmarket.loandata.controller;

import com.onmarket.loandata.domain.LoanProduct;
import com.onmarket.loandata.service.LoanProductService;
import com.onmarket.response.ApiResponse;
import com.onmarket.response.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses; // ApiResponses 임포트
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Loan Product API", description = "정부 대출상품정보 API")
@RestController
@RequestMapping("/api/loan-products")
@RequiredArgsConstructor
@Slf4j
public class LoanProductController {

    private final LoanProductService loanProductService;

    @PostMapping("/fetch-all")
    @Operation(summary = "전체 대출상품 데이터 수집", description = "공공데이터포털에서 모든 대출상품 정보를 수집하여 DB에 저장")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "데이터 수집 성공 또는 실패에 대한 처리 결과 응답"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "요청 처리 중 서버 내부 오류 발생")
    })
    public ResponseEntity<ApiResponse<Void>> fetchAllData() {
        try {
            log.info("전체 대출상품 데이터 수집 시작");
            loanProductService.fetchAndSaveAllLoanProducts();
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.DATA_FETCH_SUCCESS));
        } catch (Exception e) {
            log.error("데이터 수집 실패: ", e);
            return ResponseEntity
                    .status(ResponseCode.DATA_FETCH_FAILURE.getHttpStatus())
                    .body(ApiResponse.fail(ResponseCode.DATA_FETCH_FAILURE, e.getMessage()));
        }
    }

    @GetMapping("/status")
    @Operation(summary = "데이터 수집 상태 확인", description = "현재 저장된 대출상품 데이터 개수 확인")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "데이터 상태 조회 성공")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDataStatus() {
        Map<String, Object> status = loanProductService.getDataStatus();
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.DATA_STATUS_READ_SUCCESS, status));
    }

    @GetMapping("/products/all")
    @Operation(summary = "전체 대출상품 조회", description = "페이징을 통한 전체 대출상품 목록 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상품 목록 조회 성공")
    })
    public ResponseEntity<ApiResponse<List<LoanProduct>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<LoanProduct> products = loanProductService.getAllProducts(page, size);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.PRODUCT_READ_SUCCESS, products));
    }

    @GetMapping("/products/search")
    @Operation(summary = "상품명 검색", description = "상품명을 포함하는 대출상품 검색")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상품 목록 조회 성공")
    })
    public ResponseEntity<ApiResponse<List<LoanProduct>>> searchByProductName(
            @RequestParam String productName) {
        List<LoanProduct> products = loanProductService.searchByProductName(productName);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.PRODUCT_READ_SUCCESS, products));
    }

    @GetMapping("/products/institution")
    @Operation(summary = "취급기관별 상품 조회", description = "특정 취급기관의 대출상품 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상품 목록 조회 성공")
    })
    public ResponseEntity<ApiResponse<List<LoanProduct>>> searchByInstitution(
            @RequestParam String institution) {
        List<LoanProduct> products = loanProductService.searchByInstitution(institution);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.PRODUCT_READ_SUCCESS, products));
    }

    @GetMapping("/products/category")
    @Operation(summary = "카테고리별 상품 조회", description = "상품 카테고리별 대출상품 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상품 목록 조회 성공")
    })
    public ResponseEntity<ApiResponse<List<LoanProduct>>> getProductsByCategory(
            @RequestParam String category) {
        List<LoanProduct> products = loanProductService.getProductsByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.PRODUCT_READ_SUCCESS, products));
    }

    @GetMapping("/products/{seq}")
    @Operation(summary = "상품 상세 조회", description = "특정 대출상품의 상세 정보 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상품 상세 정보 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 seq의 상품을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<LoanProduct>> getProductBySeq(@PathVariable String seq) {
        try {
            LoanProduct product = loanProductService.getProductBySeq(seq);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.PRODUCT_READ_SUCCESS, product));
        } catch (EntityNotFoundException e) {
            return ResponseEntity
                    .status(ResponseCode.MEMBER_NOT_FOUND.getHttpStatus()) // 404 Not Found
                    .body(ApiResponse.fail(ResponseCode.MEMBER_NOT_FOUND, "해당 seq의 상품을 찾을 수 없습니다: " + seq));
        }
    }

}
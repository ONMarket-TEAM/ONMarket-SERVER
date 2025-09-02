package com.onmarket.loandata.controller;

import com.onmarket.common.response.ApiResponse;
import com.onmarket.common.response.ResponseCode;
import com.onmarket.loandata.domain.LoanProduct;
import com.onmarket.loandata.service.LoanProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ApiResponse<Void> fetchAllData() {
        try {
            log.info("전체 대출상품 데이터 수집 시작");
            loanProductService.fetchAndSaveAllLoanProducts();
            return ApiResponse.success(ResponseCode.DATA_FETCH_SUCCESS);
        } catch (Exception e) {
            log.error("데이터 수집 실패: ", e);
            return ApiResponse.fail(ResponseCode.DATA_FETCH_FAILURE, e.getMessage());
        }
    }

    @GetMapping("/status")
    @Operation(summary = "데이터 수집 상태 확인", description = "현재 저장된 대출상품 데이터 개수 확인")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "데이터 상태 조회 성공")
    })
    public ApiResponse<Map<String, Object>> getDataStatus() {
        try {
            Map<String, Object> status = loanProductService.getDataStatus();
            return ApiResponse.success(ResponseCode.DATA_STATUS_READ_SUCCESS, status);
        } catch (Exception e) {
            log.error("데이터 상태 조회 실패: ", e);
            return ApiResponse.fail(ResponseCode.DATABASE_ERROR, e.getMessage());
        }
    }

    @GetMapping("/products/all")
    @Operation(summary = "전체 대출상품 조회", description = "페이징을 통한 전체 대출상품 목록 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상품 목록 조회 성공")
    })
    public ApiResponse<List<LoanProduct>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<LoanProduct> products = loanProductService.getAllProducts(page, size);
            return ApiResponse.success(ResponseCode.PRODUCT_READ_SUCCESS, products);
        } catch (Exception e) {
            log.error("전체 대출상품 조회 실패: ", e);
            return ApiResponse.fail(ResponseCode.DATABASE_ERROR, e.getMessage());
        }
    }

    @GetMapping("/products/search")
    @Operation(summary = "상품명 검색", description = "상품명을 포함하는 대출상품 검색")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상품 목록 조회 성공")
    })
    public ApiResponse<List<LoanProduct>> searchByProductName(@RequestParam String productName) {
        try {
            List<LoanProduct> products = loanProductService.searchByProductName(productName);
            return ApiResponse.success(ResponseCode.PRODUCT_READ_SUCCESS, products);
        } catch (Exception e) {
            log.error("상품명 검색 실패: ", e);
            return ApiResponse.fail(ResponseCode.DATABASE_ERROR, e.getMessage());
        }
    }

//    @GetMapping("/products/institution")
//    @Operation(summary = "취급기관별 상품 조회", description = "특정 취급기관의 대출상품 조회")
//    @ApiResponses({
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상품 목록 조회 성공")
//    })
//    public ApiResponse<List<LoanProduct>> searchByInstitution(@RequestParam String institution) {
//        try {
//            List<LoanProduct> products = loanProductService.searchByInstitution(institution);
//            return ApiResponse.success(ResponseCode.PRODUCT_READ_SUCCESS, products);
//        } catch (Exception e) {
//            log.error("취급기관별 상품 조회 실패: ", e);
//            return ApiResponse.fail(ResponseCode.DATABASE_ERROR, e.getMessage());
//        }
//    }

//    @GetMapping("/products/category")
//    @Operation(summary = "카테고리별 상품 조회", description = "상품 카테고리별 대출상품 조회")
//    @ApiResponses({
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상품 목록 조회 성공")
//    })
//    public ApiResponse<List<LoanProduct>> getProductsByCategory(@RequestParam String category) {
//        try {
//            List<LoanProduct> products = loanProductService.getProductsByCategory(category);
//            return ApiResponse.success(ResponseCode.PRODUCT_READ_SUCCESS, products);
//        } catch (Exception e) {
//            log.error("카테고리별 상품 조회 실패: ", e);
//            return ApiResponse.fail(ResponseCode.DATABASE_ERROR, e.getMessage());
//        }
//    }

    @GetMapping("/products/{seq}")
    @Operation(summary = "상품 상세 조회", description = "특정 대출상품의 상세 정보 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상품 상세 정보 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 seq의 상품을 찾을 수 없음")
    })
    public ApiResponse<LoanProduct> getProductBySeq(@PathVariable String seq) {
        try {
            LoanProduct product = loanProductService.getProductBySeq(seq);
            return ApiResponse.success(ResponseCode.PRODUCT_READ_SUCCESS, product);
        } catch (EntityNotFoundException e) {
            log.warn("상품 조회 실패 - seq: {}, 오류: {}", seq, e.getMessage());
            return ApiResponse.fail(ResponseCode.PRODUCT_NOT_FOUND, "해당 seq의 상품을 찾을 수 없습니다: " + seq);
        } catch (Exception e) {
            log.error("상품 상세 조회 중 오류 발생: ", e);
            return ApiResponse.fail(ResponseCode.DATABASE_ERROR, e.getMessage());
        }
    }
}
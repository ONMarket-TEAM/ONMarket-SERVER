package com.onmarket.fssdata.controller;

import com.onmarket.fssdata.domain.CreditLoanOption;
import com.onmarket.fssdata.domain.CreditLoanProduct;
import com.onmarket.fssdata.service.CreditLoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Tag(name = "CreditLoan API", description = "개인신용대출 API")
@RestController
@RequestMapping("/api/credit-loans")
@RequiredArgsConstructor
@Slf4j
public class CreditLoanController {

    private final CreditLoanService creditLoanService;

    // 데이터 수집 API - 모든 권역
    @PostMapping("/fetch-all")
    @Operation(summary = "전체 데이터 조회", description = "모든 개인신용대출 상품을 가져옵니다.")

    public ResponseEntity<Map<String, Object>> fetchData() {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("전체 권역 데이터 수집 시작");
            creditLoanService.fetchAndSaveAllCreditLoanData();

            response.put("success", true);
            response.put("message", "모든 권역 데이터 수집 완료");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("데이터 수집 실패: ", e);
            response.put("success", false);
            response.put("message", "데이터 수집 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // 특정 권역 데이터 수집
    @PostMapping("/fetch/{topFinGrpNo}")
    @Operation(summary = "특정 데이터 조회", description = "020000: 은행\n" +
            "030200: 여신전문금융회사\n" +
            "030300: 저축은행\n" +
            "050000: 보험회사\n" +
            "060000: 금융투자회사 조회")

    public ResponseEntity<Map<String, Object>> fetchDataByTopFinGrp(@PathVariable String topFinGrpNo) {
        Map<String, Object> response = new HashMap<>();
        String sectorName = getSectorName(topFinGrpNo);

        try {
            log.info("권역 {} ({}) 데이터 수집 시작", topFinGrpNo, sectorName);
            creditLoanService.fetchAndSaveCreditLoanData(topFinGrpNo);

            response.put("success", true);
            response.put("message", sectorName + " 데이터 수집 완료");
            response.put("topFinGrpNo", topFinGrpNo);
            response.put("sectorName", sectorName);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("권역 {} 데이터 수집 실패: ", topFinGrpNo, e);
            response.put("success", false);
            response.put("message", sectorName + " 데이터 수집 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // 데이터 수집 상태 확인
    @GetMapping("/status")
    @Operation(summary = "데이터 수집 상태 확인", description = "데이터 수집 상태 개수를 확인합니다")
    public ResponseEntity<Map<String, Object>> getDataStatus() {
        Map<String, Object> status = new HashMap<>();

        long totalProducts = creditLoanService.getTotalProductCount();
        long totalOptions = creditLoanService.getTotalOptionCount();

        status.put("totalProducts", totalProducts);
        status.put("totalOptions", totalOptions);
        status.put("isEmpty", totalProducts == 0);

        return ResponseEntity.ok(status);
    }



    // 사용자용 API

    // 금융회사별 상품 조회
    @GetMapping("/products")
    public ResponseEntity<List<CreditLoanProduct>> getProductsByCompany(
            @RequestParam String companyName) {
        List<CreditLoanProduct> products = creditLoanService.getProductsByCompany(companyName);
        return ResponseEntity.ok(products);
    }

    // 상품별 금리 옵션 조회
    @GetMapping("/options/{finPrdtCd}")
    public ResponseEntity<List<CreditLoanOption>> getOptionsByProduct(
            @PathVariable String finPrdtCd) {
        List<CreditLoanOption> options = creditLoanService.getOptionsByProduct(finPrdtCd);
        return ResponseEntity.ok(options);
    }

    // 모든 상품 조회 (페이징)
    @GetMapping("/products/all")
    public ResponseEntity<List<CreditLoanProduct>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<CreditLoanProduct> products = creditLoanService.getAllProducts(page, size);
        return ResponseEntity.ok(products);
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
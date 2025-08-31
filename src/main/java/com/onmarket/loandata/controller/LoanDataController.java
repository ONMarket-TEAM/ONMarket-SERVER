package com.onmarket.loandata.controller;

import com.onmarket.loandata.service.LoanDataService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
@Tag(name = "Loan API", description = "대출금 API")
@RestController
@RequestMapping("/api/loan-data")
@RequiredArgsConstructor
public class LoanDataController {

    private final LoanDataService loanDataService;

    @PostMapping("/fetch-all")
    public Mono<ResponseEntity<String>> fetchAllLoanData() {
        return loanDataService.fetchAndSaveLoanProducts()
                .then(Mono.just(ResponseEntity.ok("Loan product data fetch started.")))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body("Failed to start fetch: " + e.getMessage())));
    }
}
package com.onmarket.supportsdata.controller;

import com.onmarket.supportsdata.service.PublicDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
@Tag(name = "Support API", description = "정부 지원금 API")
@RestController
@RequestMapping("/api/public-data")
@RequiredArgsConstructor
public class PublicDataController {

    private final PublicDataService publicDataService;

    @PostMapping("/fetch-all") //
    @Operation(summary = "전체 데이터 조회", description = "모든 정부지원금 상품을 가져옵니다.")

    public Mono<ResponseEntity<String>> fetchAndSaveAllData() {
        return publicDataService.findAndSaveAllServices() // ◀◀-- 변경된 메소드 호출
                .then(Mono.just(ResponseEntity.ok("Data fetch and save process started successfully."))) // ◀◀-- 작업 시작 성공 메시지 반환
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body("Failed to start data fetch process: " + e.getMessage())));
    }
}
package com.onmarket.supportsdata.controller;

import com.onmarket.supportsdata.service.PublicDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/public-data")
@RequiredArgsConstructor
public class PublicDataController {

    private final PublicDataService publicDataService;

    @PostMapping("/fetch-all") // ◀◀-- 역할에 맞게 URL 변경 추천
    public Mono<ResponseEntity<String>> fetchAndSaveAllData() {
        return publicDataService.findAndSaveAllServices() // ◀◀-- 변경된 메소드 호출
                .then(Mono.just(ResponseEntity.ok("Data fetch and save process started successfully."))) // ◀◀-- 작업 시작 성공 메시지 반환
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body("Failed to start data fetch process: " + e.getMessage())));
    }
}
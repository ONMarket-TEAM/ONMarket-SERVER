package com.onmarket.business.controller;

import com.onmarket.business.dto.BusinessRequest;
import com.onmarket.business.dto.BusinessResponse;
import com.onmarket.business.service.impl.BusinessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
public class BusinessApiController {

    private final BusinessService businessService;

    // 사업장 등록
    @PostMapping("/{memberId}")
    public ResponseEntity<BusinessResponse> registerBusiness(
            @PathVariable Long memberId,
            @RequestBody BusinessRequest request) {
        return ResponseEntity.ok(businessService.registerBusiness(memberId, request));
    }

    // 특정 회원의 사업장 조회
    @GetMapping("/{memberId}")
    public ResponseEntity<List<BusinessResponse>> getMemberBusinesses(@PathVariable Long memberId) {
        return ResponseEntity.ok(businessService.getMemberBusinesses(memberId));
    }
}

package com.onmarket.business.controller;

import com.onmarket.business.dto.BusinessRequest;
import com.onmarket.business.dto.BusinessResponse;
import com.onmarket.business.service.impl.BusinessService;
import com.onmarket.common.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
public class BusinessApiController {

    private final BusinessService businessService;
    private final JwtTokenProvider jwtTokenProvider;

    // JWT 토큰에서 이메일 추출
    private String extractEmailFromToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization 헤더가 없습니다.");
        }
        String token = header.substring(7);
        return jwtTokenProvider.getEmail(token);
    }

    // 사업장 등록
    @PostMapping
    public ResponseEntity<BusinessResponse> registerBusiness(
            HttpServletRequest request,
            @RequestBody BusinessRequest businessRequest) {

        String email = extractEmailFromToken(request);
        return ResponseEntity.ok(businessService.registerBusiness(email, businessRequest));
    }

    // 회원의 사업장 조회
    @GetMapping
    public ResponseEntity<List<BusinessResponse>> getMemberBusinesses(HttpServletRequest request) {
        String email = extractEmailFromToken(request);
        return ResponseEntity.ok(businessService.getMemberBusinesses(email));
    }
}

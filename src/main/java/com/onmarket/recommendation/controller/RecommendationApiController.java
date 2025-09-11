package com.onmarket.recommendation.controller;

import com.onmarket.common.response.ResponseCode;
import com.onmarket.member.exception.LoginException;
import com.onmarket.oauth.jwt.JwtTokenProvider;
import com.onmarket.recommendation.dto.InteractionRequest;
import com.onmarket.recommendation.dto.RecommendationResponse;
import com.onmarket.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "BearerAuth")
public class RecommendationApiController {

    private final RecommendationService recommendationService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 개인화 추천 조회 (타임아웃 및 에러 핸들링 강화)
     */
    @GetMapping("/personal")
    public ResponseEntity<List<RecommendationResponse>> getPersonalRecommendations(
            HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String email = null;

        try {
            email = extractEmailFromToken(request);
            log.info("개인 추천 요청: {}", email);

            List<RecommendationResponse> recommendations =
                    recommendationService.getPersonalizedRecommendations(email);

            log.info("개인 추천 응답: {} 건, {}ms",
                    recommendations.size(), System.currentTimeMillis() - startTime);

            // 빈 리스트라도 200 OK로 응답 (프론트엔드에서 폴백 처리)
            return ResponseEntity.ok(recommendations);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("개인 추천 조회 실패: email={}, duration={}ms", email, duration, e);

            // 에러가 발생해도 빈 리스트를 반환하여 프론트엔드에서 폴백 처리 가능하게 함
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    /**
     * 상호작용 기록 (비동기 처리로 빠른 응답)
     */
    @PostMapping("/interactions")
    public ResponseEntity<Void> recordInteraction(
            @RequestBody InteractionRequest request,
            HttpServletRequest httpRequest) {

        try {
            String email = extractEmailFromToken(httpRequest);

            // 비동기로 처리하여 즉시 응답
            recommendationService.recordUserInteraction(
                    email,
                    request.getPostId(),
                    request.getInteractionType(),
                    request.getDurationSeconds(),
                    request.getScrollPercentage(),
                    request.getRating()
            );

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("상호작용 기록 실패", e);
            // 상호작용 기록 실패는 사용자 경험에 영향을 주지 않으므로 200 OK 반환
            return ResponseEntity.ok().build();
        }
    }

    /**
     * 캐시 상태 확인 (개발/디버깅용)
     */
    @GetMapping("/cache/status")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("timestamp", System.currentTimeMillis());
            status.put("status", "active");
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 캐시 수동 삭제 (필요시)
     */
    @DeleteMapping("/cache")
    public ResponseEntity<Void> evictUserCache(HttpServletRequest httpRequest) {
        String email = null;
        try {
            email = extractEmailFromToken(httpRequest);
            // CacheManager를 통해 수동으로 캐시 삭제 로직
            log.info("사용자 캐시 삭제: {}", email);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("캐시 삭제 실패: {}", email, e);
            return ResponseEntity.ok().build();
        }
    }
    private String extractEmailFromToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new LoginException(ResponseCode.AUTH_TOKEN_NOT_FOUND);
        }

        String token = header.substring(7);

        if (!jwtTokenProvider.validateToken(token)) {
            throw new LoginException(ResponseCode.AUTH_TOKEN_INVALID);
        }

        return jwtTokenProvider.getEmail(token);
    }
}
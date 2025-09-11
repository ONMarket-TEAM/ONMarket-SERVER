package com.onmarket.recommendation.controller;

import com.onmarket.common.response.ResponseCode;
import com.onmarket.member.exception.LoginException;
import com.onmarket.oauth.jwt.JwtTokenProvider;
import com.onmarket.post.domain.PostType;
import com.onmarket.recommendation.dto.InteractionRequest;
import com.onmarket.recommendation.dto.RecommendationResponse;
import com.onmarket.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "BearerAuth")
public class RecommendationApiController {

    private final RecommendationService recommendationService;
    private final JwtTokenProvider jwtTokenProvider;
    private final @Qualifier("quickCacheManager") CacheManager quickCacheManager; // 추가

    /**
     * 개인화 추천 조회 (PostType 균형 반영)
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

            // 🔥 PostType별 개수 로깅 추가
            long loanCount = recommendations.stream()
                    .filter(rec -> rec.getPostType() == PostType.LOAN)
                    .count();
            long supportCount = recommendations.stream()
                    .filter(rec -> rec.getPostType() == PostType.SUPPORT)
                    .count();

            log.info("개인 추천 응답: {} 건 (LOAN: {}, SUPPORT: {}), {}ms",
                    recommendations.size(), loanCount, supportCount,
                    System.currentTimeMillis() - startTime);

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

            // 🔥 상호작용 타입별 로깅 강화
            log.debug("상호작용 기록 요청: {} - {} - {}",
                    email, request.getPostId(), request.getInteractionType());

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
            log.error("상호작용 기록 실패: postId={}", request.getPostId(), e);
            // 상호작용 기록 실패는 사용자 경험에 영향을 주지 않으므로 200 OK 반환
            return ResponseEntity.ok().build();
        }
    }

    /**
     * 🔥 캐시 상태 확인 (상세 정보 제공)
     */
    @GetMapping("/cache/status")
    public ResponseEntity<Map<String, Object>> getCacheStatus(HttpServletRequest request) {
        try {
            String email = extractEmailFromToken(request);
            Map<String, Object> status = new HashMap<>();

            // 기본 정보
            status.put("timestamp", System.currentTimeMillis());
            status.put("status", "active");
            status.put("userEmail", email);

            // 🔥 사용자별 캐시 상태 확인
            Cache userRecommendationsCache = quickCacheManager.getCache("userRecommendations");
            if (userRecommendationsCache != null) {
                Object cachedRecommendations = userRecommendationsCache.get(email, List.class);
                status.put("hasCachedRecommendations", cachedRecommendations != null);
                if (cachedRecommendations instanceof List) {
                    status.put("cachedRecommendationsCount", ((List<?>) cachedRecommendations).size());
                }
            } else {
                status.put("hasCachedRecommendations", false);
                status.put("cacheStatus", "CACHE_NOT_FOUND");
            }

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("캐시 상태 확인 실패", e);
            Map<String, Object> errorStatus = new HashMap<>();
            errorStatus.put("status", "error");
            errorStatus.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorStatus);
        }
    }

    /**
     * 🔥 캐시 수동 삭제 (실제 삭제 로직 구현)
     */
    @DeleteMapping("/cache")
    public ResponseEntity<Map<String, Object>> evictUserCache(HttpServletRequest httpRequest) {
        String email = null;
        try {
            email = extractEmailFromToken(httpRequest);

            // 🔥 실제 캐시 삭제 로직 구현
            Cache userRecommendationsCache = quickCacheManager.getCache("userRecommendations");
            boolean cacheEvicted = false;

            if (userRecommendationsCache != null) {
                // 삭제 전 캐시 존재 여부 확인
                boolean hadCache = userRecommendationsCache.get(email, List.class) != null;

                // 캐시 삭제
                userRecommendationsCache.evict(email);
                cacheEvicted = true;

                log.info("사용자 캐시 삭제 완료: {} (기존 캐시 존재: {})", email, hadCache);

                // 응답 데이터 구성
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("email", email);
                response.put("hadCachedData", hadCache);
                response.put("timestamp", System.currentTimeMillis());

                return ResponseEntity.ok(response);
            } else {
                log.warn("캐시 매니저를 찾을 수 없음: {}", email);

                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("reason", "CACHE_MANAGER_NOT_FOUND");
                response.put("email", email);
                response.put("timestamp", System.currentTimeMillis());

                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            log.error("캐시 삭제 실패: {}", email, e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("reason", "INTERNAL_ERROR");
            response.put("email", email);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        }
    }

    /**
     * 🆕 추천 강제 갱신 (캐시 삭제 후 새 추천 생성)
     */
    @PostMapping("/refresh")
    public ResponseEntity<List<RecommendationResponse>> refreshRecommendations(
            HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String email = null;

        try {
            email = extractEmailFromToken(request);
            log.info("추천 강제 갱신 요청: {}", email);

            // 1. 기존 캐시 삭제
            Cache userRecommendationsCache = quickCacheManager.getCache("userRecommendations");
            if (userRecommendationsCache != null) {
                userRecommendationsCache.evict(email);
                log.debug("기존 캐시 삭제 완료: {}", email);
            }

            // 2. 새로운 추천 생성 (캐시에 자동 저장됨)
            List<RecommendationResponse> freshRecommendations =
                    recommendationService.getPersonalizedRecommendations(email);

            // PostType별 개수 로깅
            long loanCount = freshRecommendations.stream()
                    .filter(rec -> rec.getPostType() == PostType.LOAN)
                    .count();
            long supportCount = freshRecommendations.stream()
                    .filter(rec -> rec.getPostType() == PostType.SUPPORT)
                    .count();

            log.info("추천 강제 갱신 완료: {} 건 (LOAN: {}, SUPPORT: {}), {}ms",
                    freshRecommendations.size(), loanCount, supportCount,
                    System.currentTimeMillis() - startTime);

            return ResponseEntity.ok(freshRecommendations);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("추천 강제 갱신 실패: email={}, duration={}ms", email, duration, e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    /**
     * 🆕 추천 통계 조회 (개발/디버깅용)
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getRecommendationStats(HttpServletRequest request) {
        try {
            String email = extractEmailFromToken(request);

            // 현재 추천 목록 조회
            List<RecommendationResponse> recommendations =
                    recommendationService.getPersonalizedRecommendations(email);

            Map<String, Object> stats = new HashMap<>();
            stats.put("email", email);
            stats.put("totalRecommendations", recommendations.size());
            stats.put("timestamp", System.currentTimeMillis());

            // PostType별 통계
            Map<String, Long> typeStats = recommendations.stream()
                    .collect(Collectors.groupingBy(
                            rec -> rec.getPostType().name(),
                            Collectors.counting()
                    ));
            stats.put("postTypeBreakdown", typeStats);

            // 평균 관심도 점수
            double avgInterestScore = recommendations.stream()
                    .mapToDouble(RecommendationResponse::getInterestScore)
                    .average()
                    .orElse(0.0);
            stats.put("averageInterestScore", Math.round(avgInterestScore * 100.0) / 100.0);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("추천 통계 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
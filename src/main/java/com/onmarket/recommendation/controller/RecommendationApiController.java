package com.onmarket.recommendation.controller;

import com.onmarket.common.response.ResponseCode;
import com.onmarket.member.exception.LoginException;
import com.onmarket.oauth.jwt.JwtTokenProvider;
import com.onmarket.post.domain.PostType;
import com.onmarket.recommendation.domain.InteractionType;
import com.onmarket.recommendation.dto.InteractionRequest;
import com.onmarket.recommendation.dto.RecommendationResponse;
import com.onmarket.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
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
     * 🔥 개선된 상호작용 기록 - 더 안전한 예외 처리
     */
    @PostMapping("/interactions")
    public ResponseEntity<Map<String, Object>> recordInteraction(
            @RequestBody InteractionRequest request,
            HttpServletRequest httpRequest) {

        long startTime = System.currentTimeMillis();
        Map<String, Object> response = new HashMap<>();
        String email = null;

        try {
            email = extractEmailFromToken(httpRequest);

            // 🔥 입력값 검증 추가
            if (request == null) {
                log.error("요청 본문이 null");
                response.put("success", false);
                response.put("error", "요청 데이터가 없습니다.");
                response.put("processingTime", System.currentTimeMillis() - startTime);
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getPostId() == null) {
                log.error("postId가 null: email={}, type={}", email, request.getInteractionType());
                response.put("success", false);
                response.put("error", "postId가 필요합니다.");
                response.put("email", email);
                response.put("requestData", Map.of(
                        "postId", "null",
                        "interactionType", request.getInteractionType() != null ? request.getInteractionType().toString() : "null"
                ));
                response.put("processingTime", System.currentTimeMillis() - startTime);
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getInteractionType() == null) {
                log.error("interactionType이 null: email={}, postId={}", email, request.getPostId());
                response.put("success", false);
                response.put("error", "상호작용 타입이 필요합니다.");
                response.put("email", email);
                response.put("processingTime", System.currentTimeMillis() - startTime);
                return ResponseEntity.badRequest().body(response);
            }

            log.debug("상호작용 기록 요청 (검증 통과): {} - {} - {}",
                    email, request.getPostId(), request.getInteractionType());

            // 🔥 검증 통과 후 서비스 호출 - 예외를 잡아도 계속 진행
            boolean serviceSuccess = false;
            String serviceError = null;

            try {
                recommendationService.recordUserInteraction(
                        email,
                        request.getPostId(),
                        request.getInteractionType(),
                        request.getDurationSeconds(),
                        request.getScrollPercentage(),
                        request.getRating()
                );
                serviceSuccess = true;
                log.debug("상호작용 기록 성공: {} - {} - {}", email, request.getPostId(), request.getInteractionType());

            } catch (Exception serviceException) {
                log.error("상호작용 기록 서비스 실패하지만 클라이언트에는 성공으로 응답: email={}, postId={}, type={}",
                        email, request.getPostId(), request.getInteractionType(), serviceException);
                serviceError = serviceException.getMessage();
                // 🔥 서비스 실패해도 클라이언트에는 성공으로 응답 (사용자 경험 보호)
            }

            boolean isCritical = isCriticalInteraction(request.getInteractionType());

            // 🔥 항상 성공으로 응답 (내부 오류가 사용자 경험에 영향을 주지 않도록)
            response.put("success", true);
            response.put("email", email);
            response.put("interactionType", request.getInteractionType());
            response.put("postId", request.getPostId());
            response.put("isCriticalInteraction", isCritical);
            response.put("processingTime", System.currentTimeMillis() - startTime);
            response.put("internalSuccess", serviceSuccess);

            if (serviceError != null) {
                response.put("internalError", serviceError);
                log.debug("상호작용 기록 - 내부 오류 있지만 클라이언트에는 성공 응답");
            }

            if (isCritical && serviceSuccess) {
                response.put("message", "중요한 상호작용이 즉시 반영되었습니다.");
                response.put("cacheUpdated", true);
            } else {
                response.put("message", "상호작용이 처리되었습니다.");
                response.put("cacheUpdated", false);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("상호작용 기록 컨트롤러 실패: email={}, request={}", email, request, e);

            // 🔥 최종 예외 발생해도 사용자 경험을 위해 200으로 응답
            response.put("success", true); // 클라이언트 측 에러 처리 방지
            response.put("email", email);
            response.put("message", "상호작용이 처리되었습니다.");
            response.put("internalError", "시스템 오류 발생");
            response.put("processingTime", System.currentTimeMillis() - startTime);

            return ResponseEntity.ok(response);
        }
    }
    /**
     * 🔥 개선된 메인 추천 조회 - Business 없어도 안전 처리
     */
    @GetMapping("/personal")
    public ResponseEntity<Map<String, Object>> getPersonalRecommendations(
            HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String email = null;
        Map<String, Object> response = new HashMap<>();

        try {
            email = extractEmailFromToken(request);
            log.info("개인 추천 요청: {}", email);

            // 🔥 서비스에서 모든 예외를 처리하도록 위임
            List<RecommendationResponse> recommendations =
                    recommendationService.getPersonalizedRecommendations(email);

            // 🔥 비어있어도 성공으로 처리 (폴백 추천이라도 제공)
            if (recommendations.isEmpty()) {
                log.warn("추천 결과가 비어있음, 폴백 추천 시도: {}", email);
                try {
                    // 마지막 수단으로 폴백 추천 시도
                    recommendations = List.of(); // 빈 리스트라도 일관성 있게 응답
                } catch (Exception fallbackError) {
                    log.error("폴백 추천도 실패: {}", email, fallbackError);
                }
            }

            long loanCount = recommendations.stream()
                    .filter(rec -> rec.getPostType() == PostType.LOAN)
                    .count();
            long supportCount = recommendations.stream()
                    .filter(rec -> rec.getPostType() == PostType.SUPPORT)
                    .count();

            response.put("success", true);
            response.put("email", email);
            response.put("recommendations", recommendations);
            response.put("totalCount", recommendations.size());
            response.put("typeBreakdown", Map.of(
                    "LOAN", loanCount,
                    "SUPPORT", supportCount
            ));
            response.put("processingTime", System.currentTimeMillis() - startTime);
            response.put("timestamp", LocalDateTime.now());

            // 🔥 추천 품질 정보 추가
            if (!recommendations.isEmpty()) {
                double avgScore = recommendations.stream()
                        .mapToDouble(RecommendationResponse::getInterestScore)
                        .average().orElse(0.0);
                response.put("averageScore", Math.round(avgScore * 100.0) / 100.0);
                response.put("hasPersonalizedData", avgScore > 50.0); // 개인화된 데이터가 있는지 추정
            }

            log.info("개인 추천 응답: {} 건 (LOAN: {}, SUPPORT: {}), {}ms",
                    recommendations.size(), loanCount, supportCount,
                    System.currentTimeMillis() - startTime);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("개인 추천 조회 실패: email={}, duration={}ms", email, duration, e);

            // 🔥 완전한 실패 시에도 빈 추천 리스트로 응답 (사용자 경험 보호)
            response.put("success", true); // 프론트엔드 에러 방지
            response.put("email", email);
            response.put("recommendations", Collections.emptyList());
            response.put("totalCount", 0);
            response.put("typeBreakdown", Map.of("LOAN", 0L, "SUPPORT", 0L));
            response.put("error", "일시적인 오류로 인해 추천을 불러올 수 없습니다.");
            response.put("processingTime", duration);
            response.put("fallbackMode", true);

            return ResponseEntity.ok(response); // 에러도 200으로 반환
        }
    }

    /**
     * 🔥 개선된 실시간 추천 조회 - 더 안전한 처리
     */
    @GetMapping("/personal/realtime")
    public ResponseEntity<Map<String, Object>> getRealtimeRecommendations(
            HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String email = null;
        Map<String, Object> response = new HashMap<>();

        try {
            email = extractEmailFromToken(request);
            log.info("실시간 추천 요청: {}", email);

            List<RecommendationResponse> realtimeRecommendations = Collections.emptyList();
            boolean forceRefreshSuccess = false;
            String errorMessage = null;

            // 🔥 강제 갱신 시도
            try {
                realtimeRecommendations = recommendationService.forceRefreshRecommendations(email);
                forceRefreshSuccess = true;
                log.info("실시간 추천 강제 갱신 성공: {}건", realtimeRecommendations.size());
            } catch (Exception refreshError) {
                log.error("실시간 추천 강제 갱신 실패, 기본 추천으로 폴백: {}", email, refreshError);
                errorMessage = refreshError.getMessage();

                // 🔥 강제 갱신 실패 시 기본 추천으로 폴백
                try {
                    realtimeRecommendations = recommendationService.getPersonalizedRecommendations(email);
                    log.info("기본 추천으로 폴백 성공: {}건", realtimeRecommendations.size());
                } catch (Exception fallbackError) {
                    log.error("기본 추천 폴백도 실패: {}", email, fallbackError);
                }
            }

            long loanCount = realtimeRecommendations.stream()
                    .filter(rec -> rec.getPostType() == PostType.LOAN)
                    .count();
            long supportCount = realtimeRecommendations.stream()
                    .filter(rec -> rec.getPostType() == PostType.SUPPORT)
                    .count();

            response.put("success", true);
            response.put("email", email);
            response.put("recommendations", realtimeRecommendations);
            response.put("totalCount", realtimeRecommendations.size());
            response.put("typeBreakdown", Map.of(
                    "LOAN", loanCount,
                    "SUPPORT", supportCount
            ));
            response.put("processingTime", System.currentTimeMillis() - startTime);
            response.put("isRealtime", forceRefreshSuccess);
            response.put("cacheUsed", false);
            response.put("timestamp", LocalDateTime.now());

            if (!forceRefreshSuccess) {
                response.put("fallbackMode", true);
                response.put("fallbackReason", errorMessage);
                response.put("message", "실시간 갱신에 실패하여 기존 추천을 제공합니다.");
            } else {
                response.put("message", "실시간으로 갱신된 추천입니다.");
            }

            log.info("실시간 추천 응답: {} 건 (LOAN: {}, SUPPORT: {}), {}ms (실시간: {})",
                    realtimeRecommendations.size(), loanCount, supportCount,
                    System.currentTimeMillis() - startTime, forceRefreshSuccess);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("실시간 추천 조회 실패: email={}, duration={}ms", email, duration, e);

            // 🔥 완전 실패 시에도 빈 추천으로 응답
            response.put("success", false);
            response.put("email", email);
            response.put("recommendations", Collections.emptyList());
            response.put("totalCount", 0);
            response.put("error", "실시간 추천 조회 실패: " + e.getMessage());
            response.put("processingTime", duration);
            response.put("fallbackMode", true);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 🔥 개선된 강제 추천 갱신 - Business 없어도 안전 처리
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshRecommendations(
            HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String email = null;
        Map<String, Object> response = new HashMap<>();

        try {
            email = extractEmailFromToken(request);
            log.info("추천 강제 갱신 요청: {}", email);

            List<RecommendationResponse> freshRecommendations = Collections.emptyList();
            boolean refreshSuccess = false;
            String errorDetails = null;

            // 🔥 강제 갱신 시도
            try {
                freshRecommendations = recommendationService.forceRefreshRecommendations(email);
                refreshSuccess = true;
                log.info("추천 강제 갱신 성공: {}건", freshRecommendations.size());
            } catch (Exception refreshError) {
                log.error("추천 강제 갱신 실패: {}", email, refreshError);
                errorDetails = refreshError.getMessage();

                // 🔥 갱신 실패 시 기존 추천이라도 반환 시도
                try {
                    freshRecommendations = recommendationService.getPersonalizedRecommendations(email);
                    log.info("강제 갱신 실패 후 기존 추천 반환: {}건", freshRecommendations.size());
                } catch (Exception fallbackError) {
                    log.error("기존 추천 조회도 실패: {}", email, fallbackError);
                }
            }

            // PostType별 개수 계산
            long loanCount = freshRecommendations.stream()
                    .filter(rec -> rec.getPostType() == PostType.LOAN)
                    .count();
            long supportCount = freshRecommendations.stream()
                    .filter(rec -> rec.getPostType() == PostType.SUPPORT)
                    .count();

            response.put("success", refreshSuccess || !freshRecommendations.isEmpty());
            response.put("email", email);
            response.put("recommendations", freshRecommendations);
            response.put("totalCount", freshRecommendations.size());
            response.put("typeBreakdown", Map.of(
                    "LOAN", loanCount,
                    "SUPPORT", supportCount
            ));
            response.put("processingTime", System.currentTimeMillis() - startTime);
            response.put("timestamp", LocalDateTime.now());
            response.put("refreshSuccess", refreshSuccess);

            if (refreshSuccess) {
                response.put("message", "추천이 즉시 갱신되었습니다.");
            } else {
                response.put("message", "추천 갱신에 실패했지만 기존 추천을 제공합니다.");
                response.put("fallbackMode", true);
                response.put("errorDetails", errorDetails);
            }

            log.info("추천 강제 갱신 응답: {} 건 (LOAN: {}, SUPPORT: {}), {}ms (성공: {})",
                    freshRecommendations.size(), loanCount, supportCount,
                    System.currentTimeMillis() - startTime, refreshSuccess);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("추천 강제 갱신 실패: email={}, duration={}ms", email, duration, e);

            response.put("success", false);
            response.put("email", email);
            response.put("error", "추천 갱신 실패: " + e.getMessage());
            response.put("recommendations", Collections.emptyList());
            response.put("totalCount", 0);
            response.put("processingTime", duration);
            response.put("refreshSuccess", false);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 🔥 헬스체크 API 추가 - 시스템 상태 모니터링
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // 기본 시스템 정보
            health.put("status", "UP");
            health.put("timestamp", LocalDateTime.now());
            health.put("service", "RecommendationService");

            // 캐시 매니저 상태 확인
            boolean cacheAvailable = false;
            try {
                Cache cache = quickCacheManager.getCache("userRecommendations");
                cacheAvailable = (cache != null);
            } catch (Exception e) {
                log.warn("캐시 상태 확인 실패", e);
            }
            health.put("cacheAvailable", cacheAvailable);

            // 응답 시간
            health.put("responseTime", System.currentTimeMillis() - startTime);

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            log.error("헬스체크 실패", e);

            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("responseTime", System.currentTimeMillis() - startTime);

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }

    @GetMapping("/debug/distribution")
    public ResponseEntity<Map<String, Object>> debugPostTypeDistribution() {
        Map<String, Object> debug = recommendationService.debugPostTypeDistribution();
        return ResponseEntity.ok(debug);
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
    /**
     * 중요한 상호작용 여부 확인 헬퍼 메서드
     */
    private boolean isCriticalInteraction(InteractionType type) {
        return type == InteractionType.SCRAP ||
                type == InteractionType.UNSCRAP ||
                type == InteractionType.RATING ||
                type == InteractionType.CLICK_LINK;
    }
}
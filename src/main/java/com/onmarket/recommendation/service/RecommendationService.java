package com.onmarket.recommendation.service;

import com.onmarket.business.domain.Business;
import com.onmarket.business.domain.enums.BusinessStatus;
import com.onmarket.business.exception.BusinessException;
import com.onmarket.business.repository.BusinessRepository;
import com.onmarket.common.response.ResponseCode;
import com.onmarket.member.domain.Member;
import com.onmarket.member.service.MemberService;
import com.onmarket.post.domain.Post;
import com.onmarket.post.domain.PostType;
import com.onmarket.post.exception.PostNotFoundException;
import com.onmarket.post.repository.PostRepository;
import com.onmarket.recommendation.domain.InteractionType;
import com.onmarket.recommendation.domain.InterestScore;
import com.onmarket.recommendation.domain.UserInteraction;
import com.onmarket.recommendation.dto.RecommendationResponse;
import com.onmarket.recommendation.repository.InterestScoreRepository;
import com.onmarket.recommendation.repository.UserInteractionRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RecommendationService {

    private final @Qualifier("quickCacheManager") CacheManager quickCacheManager;

    private final PostRepository postRepository;
    private final BusinessRepository businessRepository;
    private final MemberService memberService;
    private final InterestScoreRepository interestScoreRepository;
    private final UserInteractionRepository userInteractionRepository;
    private final PriorityRecommendationService priorityRecommendationService;

    // 성능 설정
    private static final int FALLBACK_LIMIT = 5; // 폴백 시 반환할 개수
    private static final int FINAL_RECOMMENDATION_COUNT = 5; // 최종 추천 개수

    private List<RecommendationResponse> getCachedRecommendations(String email) {
        return Optional.ofNullable(quickCacheManager.getCache("userRecommendations"))
                .map(cache -> cache.get(email, List.class))
                .orElse(Collections.emptyList());
    }

    private void putCache(String email, List<RecommendationResponse> recommendations) {
        Optional.ofNullable(quickCacheManager.getCache("userRecommendations"))
                .ifPresent(cache -> cache.put(email, recommendations));
    }

    /**
     * 🔥 우선순위 기반 개인화 추천 메인 로직
     */
    public List<RecommendationResponse> getPersonalizedRecommendations(String email) {
        long startTime = System.currentTimeMillis();
        log.info("우선순위 기반 개인화 추천 시작: {}", email);

        try {
            Member member = memberService.findByEmail(email);
            Business business = findBusinessById(member.getMainBusinessId());

            // 1️⃣ 캐시 확인
            List<RecommendationResponse> cached = getCachedRecommendations(email);
            if (!cached.isEmpty()) {
                log.info("캐시에서 추천 반환: {}건", cached.size());
                return cached;
            }

            // 2️⃣ 모든 게시물 조회 (최신 순으로 제한된 수)
            List<Post> allPosts = postRepository.findTop50ByOrderByCreatedAtDesc();

            if (allPosts.isEmpty()) {
                log.warn("분석할 게시물이 없음, 폴백 실행");
                return getFallbackRecommendations();
            }

            // 3️⃣ 우선순위 기반 추천 생성
            List<RecommendationResponse> recommendations = priorityRecommendationService
                    .generatePriorityRecommendations(member, business, allPosts, FINAL_RECOMMENDATION_COUNT);

            // 4️⃣ 추천 결과가 부족한 경우 폴백 추가
            if (recommendations.size() < FALLBACK_LIMIT) {
                log.info("추천 결과 부족({}건), 폴백 추가", recommendations.size());

                List<RecommendationResponse> fallback = getFallbackRecommendations();
                Set<Long> existingPostIds = recommendations.stream()
                        .map(RecommendationResponse::getPostId)
                        .collect(Collectors.toSet());

                List<RecommendationResponse> additionalRecs = fallback.stream()
                        .filter(rec -> !existingPostIds.contains(rec.getPostId()))
                        .limit(FALLBACK_LIMIT - recommendations.size())
                        .collect(Collectors.toList());

                recommendations.addAll(additionalRecs);
            }

            // 5️⃣ 최종 결과 정리
            List<RecommendationResponse> finalRecommendations = recommendations.stream()
                    .limit(FINAL_RECOMMENDATION_COUNT)
                    .collect(Collectors.toList());

            log.info("최종 추천 완료: {}건 반환", finalRecommendations.size());
            putCache(email, finalRecommendations);
            return finalRecommendations;

        } catch (Exception e) {
            log.error("추천 생성 실패, 폴백 실행: {}", email, e);
            List<RecommendationResponse> fallback = getFallbackRecommendations();
            putCache(email, fallback);
            return fallback;
        } finally {
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("추천 처리 완료: {}ms", totalTime);
        }
    }

    /**
     * 기본 추천 초기화 (신규 사용자용)
     */
    @Transactional(readOnly = false)
    public void initializeBasicRecommendations(Member member, Business business) {
        log.info("신규 사용자 기본 추천 초기화: {}", member.getEmail());

        try {
            // 최신 게시물 조회
            List<Post> recentPosts = postRepository.findTop20ByOrderByCreatedAtDesc();

            // 우선순위 기반으로 상위 N개 선택
            List<RecommendationResponse> priorityRecs = priorityRecommendationService
                    .generatePriorityRecommendations(member, business, recentPosts, FALLBACK_LIMIT);

            // 선택된 게시물들에 대해 기본 스코어 생성
            for (RecommendationResponse rec : priorityRecs) {
                Post post = postRepository.findById(rec.getPostId()).orElse(null);
                if (post == null) continue;

                Optional<InterestScore> existing = interestScoreRepository
                        .findByMemberAndBusinessAndPost(member, business, post);

                if (existing.isEmpty()) {
                    double baseScore = calculateBaseScore(post);

                    InterestScore basicScore = InterestScore.builder()
                            .member(member)
                            .business(business)
                            .post(post)
                            .viewScore(baseScore * 0.3)
                            .engagementScore(0.0)
                            .timeScore(baseScore * 0.7)
                            .totalScore(baseScore)
                            .lastCalculatedAt(LocalDateTime.now())
                            .build();

                    interestScoreRepository.save(basicScore);
                }
            }

            log.info("기본 추천 초기화 완료: {}건", priorityRecs.size());

        } catch (Exception e) {
            log.error("기본 추천 초기화 실패", e);
        }
    }

    /**
     * PostType별 기본 스코어 계산
     */
    private double calculateBaseScore(Post post) {
        double baseScore = 15.0;

        // PostType별 가중치
        switch (post.getPostType()) {
            case LOAN -> baseScore += 5.0;
            case SUPPORT -> baseScore += 3.0;
            default -> baseScore += 0.0;
        }

        // 최신성 보너스
        if (post.getCreatedAt() != null) {
            long daysSinceCreated = java.time.Duration.between(
                    post.getCreatedAt(), LocalDateTime.now()).toDays();
            double recencyBonus = Math.max(0, 10.0 - daysSinceCreated * 0.5);
            baseScore += recencyBonus;
        }

        return baseScore;
    }

    /**
     * 최종 폴백 추천 (항상 결과 반환 보장)
     */
    private List<RecommendationResponse> getFallbackRecommendations() {
        try {
            log.info("폴백 추천 생성");

            // 타입별로 균형있게 선택
            List<Post> loanPosts = postRepository.findTopByPostTypeOrderByCreatedAtDesc(
                    PostType.LOAN, PageRequest.of(0, 6));
            List<Post> supportPosts = postRepository.findTopByPostTypeOrderByCreatedAtDesc(
                    PostType.SUPPORT, PageRequest.of(0, 4));

            List<Post> allFallbackPosts = new ArrayList<>();
            allFallbackPosts.addAll(loanPosts);
            allFallbackPosts.addAll(supportPosts);

            // 최신 순으로 정렬하고 제한
            List<RecommendationResponse> fallbackRecs = allFallbackPosts.stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .limit(FALLBACK_LIMIT)
                    .map(post -> RecommendationResponse.builder()
                            .postId(post.getPostId())
                            .productName(post.getProductName())
                            .companyName(post.getCompanyName())
                            .postType(post.getPostType())
                            .deadline(post.getDeadline())
                            .summary(post.getSummary())
                            .imageUrl(post.getImageUrl())
                            .interestScore(0.0)
                            .recommendationReason("추천 상품")
                            .build())
                    .collect(Collectors.toList());

            log.info("폴백 추천 생성 완료: {}건", fallbackRecs.size());
            return fallbackRecs;

        } catch (Exception e) {
            log.error("폴백 추천 생성도 실패", e);
            return Collections.emptyList();
        }
    }

    /**
     * 비동기 상호작용 기록
     */
    @Async
    @Transactional(readOnly = false)
    public void recordUserInteraction(String email, Long postId, InteractionType type,
                                      Integer duration, Integer scrollPercentage, Integer rating) {
        try {
            Member member = memberService.findByEmail(email);
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new PostNotFoundException());

            UserInteraction interaction = UserInteraction.builder()
                    .member(member)
                    .post(post)
                    .interactionType(type)
                    .durationSeconds(duration)
                    .scrollPercentage(scrollPercentage)
                    .rating(rating)
                    .build();

            userInteractionRepository.save(interaction);
            log.debug("상호작용 기록: {} - {} - {}", email, postId, type);

            // 캐시 무효화 (상호작용 발생 시 추천 갱신)
            Optional.ofNullable(quickCacheManager.getCache("userRecommendations"))
                    .ifPresent(cache -> cache.evict(email));

            // 스코어 업데이트는 백그라운드에서
            updateSinglePostScoreAsync(member, post);

        } catch (Exception e) {
            log.error("상호작용 기록 실패: email={}, postId={}", email, postId, e);
        }
    }

    /**
     * 비동기 스코어 업데이트
     */
    @Async
    protected void updateSinglePostScoreAsync(Member member, Post post) {
        try {
            updateSinglePostScore(member, post);
        } catch (Exception e) {
            log.error("비동기 스코어 업데이트 실패: postId={}", post.getPostId(), e);
        }
    }

    /**
     * 단일 게시물 관심도 스코어 업데이트
     */
    @Transactional(readOnly = false)
    public void updateSinglePostScore(Member member, Post post) {
        Business business = businessRepository.findByMember(member).stream()
                .filter(b -> b.getStatus() == BusinessStatus.ACTIVE)
                .findFirst()
                .orElse(null);

        if (business == null) return;

        List<UserInteraction> interactions = userInteractionRepository
                .findByMemberAndPost(member, post);

        // 스코어 계산
        Double viewScore = calculateViewScore(interactions);
        Double engagementScore = calculateEngagementScore(interactions);
        Double timeScore = calculateTimeScore(interactions);

        // 기존 스코어 조회 또는 생성
        InterestScore score = interestScoreRepository
                .findByMemberAndBusinessAndPost(member, business, post)
                .orElse(InterestScore.builder()
                        .member(member)
                        .business(business)
                        .post(post)
                        .build());

        score.updateScore(viewScore, engagementScore, timeScore);
        interestScoreRepository.save(score);

        log.debug("스코어 업데이트 완료: Post({}) - Total: {}", post.getPostId(), score.getTotalScore());
    }

    /**
     * 조회 기반 스코어 계산
     */
    private Double calculateViewScore(List<UserInteraction> interactions) {
        long viewCount = interactions.stream()
                .filter(i -> i.getInteractionType() == InteractionType.VIEW)
                .count();
        return Math.min(viewCount * 10.0, 100.0);
    }

    /**
     * 참여 기반 스코어 계산
     */
    private Double calculateEngagementScore(List<UserInteraction> interactions) {
        double score = 0.0;

        for (UserInteraction interaction : interactions) {
            switch (interaction.getInteractionType()) {
                case SCRAP -> score += 50.0;
                case UNSCRAP -> score -= 25.0;
                case RATING -> {
                    if (interaction.getRating() != null) {
                        score += interaction.getRating() * 10.0;
                    }
                }
                case COMMENT -> score += 30.0;
                case CLICK_LINK -> score += 40.0;
            }
        }
        return Math.min(score, 200.0);
    }

    /**
     * 체류시간 기반 스코어 계산
     */
    private Double calculateTimeScore(List<UserInteraction> interactions) {
        int totalDuration = interactions.stream()
                .filter(i -> i.getDurationSeconds() != null)
                .mapToInt(UserInteraction::getDurationSeconds)
                .sum();

        int maxScroll = interactions.stream()
                .filter(i -> i.getScrollPercentage() != null)
                .mapToInt(UserInteraction::getScrollPercentage)
                .max()
                .orElse(0);

        double timeScore = Math.min((totalDuration / 30.0) * 10.0, 60.0);
        double scrollScore = (maxScroll / 100.0) * 40.0;

        return timeScore + scrollScore;
    }

    /**
     * 사업장 정보 변경 시 추천 캐시 재구축
     */
    @Transactional(readOnly = false)
    public void rebuildRecommendationsForBusiness(Member member, Business business) {
        log.info("사업장 변경으로 인한 추천 재구축: Member {}, Business {}",
                member.getEmail(), business.getBusinessId());

        try {
            // 캐시 무효화
            Optional.ofNullable(quickCacheManager.getCache("userRecommendations"))
                    .ifPresent(cache -> cache.evict(member.getEmail()));

            // 새로운 추천 생성 (비동기)
            generateNewRecommendationsAsync(member.getEmail());

            log.info("추천 재구축 완료");

        } catch (Exception e) {
            log.error("추천 재구축 실패", e);
        }
    }

    @Async
    protected void generateNewRecommendationsAsync(String email) {
        try {
            // 새로운 추천 생성하여 캐시에 저장
            getPersonalizedRecommendations(email);
        } catch (Exception e) {
            log.error("비동기 추천 생성 실패: {}", email, e);
        }
    }

    private Business findBusinessById(Long businessId) {
        return businessRepository.findById(businessId)
                .orElseThrow(() -> new BusinessException(ResponseCode.BUSINESS_NOT_FOUND));
    }
}
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
import com.onmarket.recommendation.service.AgeFilterService;
import com.onmarket.recommendation.service.PriorityRecommendationService;
import com.onmarket.recommendation.service.RegionFilterService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
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
    private final RegionFilterService regionFilterService;
    private final AgeFilterService ageFilterService;

    // 성능 설정
    private static final int FALLBACK_LIMIT = 5;
    private static final int FINAL_RECOMMENDATION_COUNT = 5;
    private static final int REGION_FILTERED_LIMIT = 100;

    // 📊 PostType별 추천 개수 설정
    private static final int LOAN_RECOMMENDATION_COUNT = 2;
    private static final int SUPPORT_RECOMMENDATION_COUNT = 3;

    /**
     * 🔥 균형 잡힌 PostType 추천 메인 로직
     */
    public List<RecommendationResponse> getPersonalizedRecommendations(String email) {
        long startTime = System.currentTimeMillis();
        log.info("균형 잡힌 추천 시작: {}", email);

        try {
            Member member = memberService.findByEmail(email);

            if (member.getMainBusinessId() == null) {
                log.warn("대표 사업장이 없는 사용자: {}, 폴백 실행", email);
                return getFallbackRecommendations();
            }

            Business business = findBusinessById(member.getMainBusinessId());

            // 1️⃣ 캐시 확인
            List<RecommendationResponse> cached = getCachedRecommendations(email);
            if (!cached.isEmpty()) {
                log.info("캐시에서 추천 반환: {}건", cached.size());
                return cached;
            }

            // 2️⃣ 지역 기반 필터링된 게시물 조회
            List<Post> regionFilteredPosts = getRegionFilteredPosts(business, member);

            if (regionFilteredPosts.isEmpty()) {
                log.warn("지역 필터링된 게시물이 없음, 전체 게시물로 폴백");
                regionFilteredPosts = postRepository.findTop50ByOrderByCreatedAtDesc();
            }

            if (regionFilteredPosts.isEmpty()) {
                log.warn("분석할 게시물이 없음, 폴백 실행");
                return getFallbackRecommendations();
            }

            log.info("지역 필터링 완료: {} -> {}건", business.getSidoName(), regionFilteredPosts.size());

            // 3️⃣ 📊 PostType별 균형 잡힌 추천 생성
            List<RecommendationResponse> balancedRecommendations = generateBalancedRecommendations(
                    member, business, regionFilteredPosts);

            // 4️⃣ 추천 결과가 부족한 경우 폴백으로 보충
            if (balancedRecommendations.size() < FINAL_RECOMMENDATION_COUNT) {
                log.info("균형 추천 결과 부족({}건), 폴백으로 보충", balancedRecommendations.size());
                enhanceWithFallback(balancedRecommendations);
            }

            // 5️⃣ 최종 결과 정리
            List<RecommendationResponse> finalRecommendations = balancedRecommendations.stream()
                    .limit(FINAL_RECOMMENDATION_COUNT)
                    .collect(Collectors.toList());

            log.info("균형 추천 완료: {}건 반환 (LOAN: {}, SUPPORT: {}, 지역: {} {})",
                    finalRecommendations.size(),
                    countByPostType(finalRecommendations, PostType.LOAN),
                    countByPostType(finalRecommendations, PostType.SUPPORT),
                    business.getSidoName(), business.getSigunguName());

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
     * 📊 PostType별 균형 잡힌 추천 생성
     */
    private List<RecommendationResponse> generateBalancedRecommendations(
            Member member, Business business, List<Post> filteredPosts) {

        log.info("PostType별 균형 추천 생성: LOAN {}개, SUPPORT {}개 목표",
                LOAN_RECOMMENDATION_COUNT, SUPPORT_RECOMMENDATION_COUNT);

        // 1. PostType별로 게시물 분리
        Map<PostType, List<Post>> postsByType = filteredPosts.stream()
                .collect(Collectors.groupingBy(Post::getPostType));

        List<Post> loanPosts = postsByType.getOrDefault(PostType.LOAN, Collections.emptyList());
        List<Post> supportPosts = postsByType.getOrDefault(PostType.SUPPORT, Collections.emptyList());

        log.info("PostType별 필터링된 게시물 수: LOAN {}개, SUPPORT {}개",
                loanPosts.size(), supportPosts.size());

        List<RecommendationResponse> balancedResults = new ArrayList<>();

        // 2. LOAN 타입에서 상위 N개 선택
        List<RecommendationResponse> loanRecommendations = selectTopRecommendationsByType(
                member, business, loanPosts, LOAN_RECOMMENDATION_COUNT, PostType.LOAN);
        balancedResults.addAll(loanRecommendations);

        // 3. SUPPORT 타입에서 상위 N개 선택
        List<RecommendationResponse> supportRecommendations = selectTopRecommendationsByType(
                member, business, supportPosts, SUPPORT_RECOMMENDATION_COUNT, PostType.SUPPORT);
        balancedResults.addAll(supportRecommendations);

        // 4. 부족한 경우 다른 타입으로 보충
        if (balancedResults.size() < FINAL_RECOMMENDATION_COUNT) {
            int needed = FINAL_RECOMMENDATION_COUNT - balancedResults.size();
            log.info("추천 부족으로 다른 타입으로 보충: {}개 필요", needed);

            Set<Long> existingIds = balancedResults.stream()
                    .map(RecommendationResponse::getPostId)
                    .collect(Collectors.toSet());

            // 전체 게시물에서 추가 선택 (기존 선택된 것 제외)
            List<Post> remainingPosts = filteredPosts.stream()
                    .filter(post -> !existingIds.contains(post.getPostId()))
                    .collect(Collectors.toList());

            List<RecommendationResponse> additionalRecs = priorityRecommendationService
                    .generatePriorityRecommendations(member, business, remainingPosts, needed);

            balancedResults.addAll(additionalRecs);
        }

        log.info("균형 추천 생성 완료: 총 {}건 (LOAN: {}, SUPPORT: {})",
                balancedResults.size(),
                countByPostType(balancedResults, PostType.LOAN),
                countByPostType(balancedResults, PostType.SUPPORT));

        return balancedResults;
    }

    /**
     * 특정 PostType에서 상위 추천 선택
     */
    private List<RecommendationResponse> selectTopRecommendationsByType(
            Member member, Business business, List<Post> posts, int count, PostType type) {

        if (posts.isEmpty()) {
            log.warn("{} 타입의 게시물이 없음", type);
            return Collections.emptyList();
        }

        try {
            // 해당 타입의 게시물들에 대해 우선순위 점수 계산
            List<RecommendationResponse> typeRecommendations = priorityRecommendationService
                    .generatePriorityRecommendations(member, business, posts, count);

            log.info("{} 타입 추천 선택: {}개 중 {}개 선택",
                    type, posts.size(), typeRecommendations.size());

            return typeRecommendations;

        } catch (Exception e) {
            log.error("{} 타입 추천 생성 실패", type, e);
            return Collections.emptyList();
        }
    }

    /**
     * PostType별 개수 계산
     */
    private long countByPostType(List<RecommendationResponse> recommendations, PostType type) {
        return recommendations.stream()
                .filter(rec -> rec.getPostType() == type)
                .count();
    }

    /**
     * 📊 균형 잡힌 폴백 추천 생성
     */
    private List<RecommendationResponse> getFallbackRecommendations() {
        try {
            log.info("균형 잡힌 폴백 추천 생성");

            // LOAN 2개, SUPPORT 3개로 구성
            List<Post> loanPosts = postRepository.findTopByPostTypeOrderByCreatedAtDesc(
                    PostType.LOAN, PageRequest.of(0, LOAN_RECOMMENDATION_COUNT * 2)); // 여유분 확보
            List<Post> supportPosts = postRepository.findTopByPostTypeOrderByCreatedAtDesc(
                    PostType.SUPPORT, PageRequest.of(0, SUPPORT_RECOMMENDATION_COUNT * 2)); // 여유분 확보

            List<RecommendationResponse> fallbackRecs = new ArrayList<>();

            // LOAN에서 2개 선택
            fallbackRecs.addAll(loanPosts.stream()
                    .limit(LOAN_RECOMMENDATION_COUNT)
                    .map(this::convertToFallbackRecommendation)
                    .collect(Collectors.toList()));

            // SUPPORT에서 3개 선택
            fallbackRecs.addAll(supportPosts.stream()
                    .limit(SUPPORT_RECOMMENDATION_COUNT)
                    .map(this::convertToFallbackRecommendation)
                    .collect(Collectors.toList()));

            // 부족한 경우 전체에서 보충
            if (fallbackRecs.size() < FINAL_RECOMMENDATION_COUNT) {
                int needed = FINAL_RECOMMENDATION_COUNT - fallbackRecs.size();
                Set<Long> existingIds = fallbackRecs.stream()
                        .map(RecommendationResponse::getPostId)
                        .collect(Collectors.toSet());

                List<Post> additionalPosts = postRepository.findTop10ByOrderByCreatedAtDesc()
                        .stream()
                        .filter(post -> !existingIds.contains(post.getPostId()))
                        .limit(needed)
                        .collect(Collectors.toList());

                fallbackRecs.addAll(additionalPosts.stream()
                        .map(this::convertToFallbackRecommendation)
                        .collect(Collectors.toList()));
            }

            log.info("균형 폴백 추천 생성 완료: {}건 (LOAN: {}, SUPPORT: {})",
                    fallbackRecs.size(),
                    countByPostType(fallbackRecs, PostType.LOAN),
                    countByPostType(fallbackRecs, PostType.SUPPORT));

            return fallbackRecs;

        } catch (Exception e) {
            log.error("균형 폴백 추천 생성 실패", e);
            return Collections.emptyList();
        }
    }

    /**
     * Post를 폴백 RecommendationResponse로 변환
     */
    private RecommendationResponse convertToFallbackRecommendation(Post post) {
        // 기본 스코어를 계산해서 0 대신 의미있는 값 설정
        double baseScore = calculateBaseScore(post);

        return RecommendationResponse.builder()
                .postId(post.getPostId())
                .productName(post.getProductName())
                .companyName(post.getCompanyName())
                .postType(post.getPostType())
                .deadline(post.getDeadline())
                .summary(post.getSummary())
                .imageUrl(post.getImageUrl())
                .interestScore(baseScore) // 0.0 대신 계산된 값 사용
                .recommendationReason("추천 상품")
                .build();
    }

    /**
     * 📊 기본 추천 초기화 (균형 고려)
     */
    @Transactional(readOnly = false)
    public void initializeBasicRecommendations(Member member, Business business) {
        log.info("균형 잡힌 기본 추천 초기화: {} (사업장: {}, 지역: {} {})",
                member.getEmail(), business.getBusinessName(),
                business.getSidoName(), business.getSigunguName());

        try {
            // 지역 필터링된 게시물 조회
            List<Post> regionFilteredPosts = getRegionFilteredPosts(business, member);

            if (regionFilteredPosts.size() < 20) {
                List<Post> additionalPosts = postRepository.findTop20ByOrderByCreatedAtDesc();
                Set<Long> existingIds = regionFilteredPosts.stream()
                        .map(Post::getPostId)
                        .collect(Collectors.toSet());

                List<Post> newPosts = additionalPosts.stream()
                        .filter(post -> !existingIds.contains(post.getPostId()))
                        .limit(20 - regionFilteredPosts.size())
                        .collect(Collectors.toList());

                regionFilteredPosts.addAll(newPosts);
            }

            // 📊 균형 잡힌 기본 추천 생성
            List<RecommendationResponse> balancedRecs = generateBalancedRecommendations(
                    member, business, regionFilteredPosts);

            // 선택된 게시물들에 대해 기본 스코어 생성
            for (RecommendationResponse rec : balancedRecs) {
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

            log.info("균형 기본 추천 초기화 완료: {}건 (LOAN: {}, SUPPORT: {})",
                    balancedRecs.size(),
                    countByPostType(balancedRecs, PostType.LOAN),
                    countByPostType(balancedRecs, PostType.SUPPORT));

        } catch (Exception e) {
            log.error("균형 기본 추천 초기화 실패", e);
        }
    }

    // 기존 메서드들은 그대로 유지...
    private List<Post> getRegionFilteredPosts(Business business, Member member) {
        try {
            List<Post> regionPosts = regionFilterService.filterPostsByRegion(
                    business.getSidoName(), business.getSigunguName());

            List<Post> filteredPosts = ageFilterService.filterPostsByAge(
                    member.getBirthDate(), regionPosts);

            List<Post> finalPosts = filteredPosts.stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .limit(REGION_FILTERED_LIMIT)
                    .collect(Collectors.toList());

            log.info("필터링 결과: 지역({} {}) & 연령({}) -> {}건",
                    business.getSidoName(), business.getSigunguName(),
                    member.getBirthDate() != null ? calculateAge(member.getBirthDate()) : "미설정",
                    finalPosts.size());

            return finalPosts;

        } catch (Exception e) {
            log.error("지역/연령 필터링 실패: {} {}", business.getSidoName(), business.getSigunguName(), e);
            return postRepository.findTop50ByOrderByCreatedAtDesc();
        }
    }

    private int calculateAge(LocalDate birthDate) {
        return java.time.Period.between(birthDate, LocalDate.now()).getYears();
    }

    private List<RecommendationResponse> getCachedRecommendations(String email) {
        return Optional.ofNullable(quickCacheManager.getCache("userRecommendations"))
                .map(cache -> cache.get(email, List.class))
                .orElse(Collections.emptyList());
    }

    private void putCache(String email, List<RecommendationResponse> recommendations) {
        Optional.ofNullable(quickCacheManager.getCache("userRecommendations"))
                .ifPresent(cache -> cache.put(email, recommendations));
    }

    private void clearMemberCache(String email) {
        Optional.ofNullable(quickCacheManager.getCache("userRecommendations"))
                .ifPresent(cache -> cache.evict(email));
    }

    private double calculateBaseScore(Post post) {
        double baseScore = 50.0; // 15.0 -> 50.0으로 상향

        switch (post.getPostType()) {
            case LOAN -> baseScore += 20.0; // 5.0 -> 20.0
            case SUPPORT -> baseScore += 15.0; // 3.0 -> 15.0
            default -> baseScore += 10.0; // 0.0 -> 10.0
        }

        if (post.getCreatedAt() != null) {
            long daysSinceCreated = java.time.Duration.between(
                    post.getCreatedAt(), LocalDateTime.now()).toDays();
            double recencyBonus = Math.max(0, 20.0 - daysSinceCreated * 0.5); // 10.0 -> 20.0
            baseScore += recencyBonus;
        }

        return baseScore;
    }

    private void enhanceWithFallback(List<RecommendationResponse> recommendations) {
        List<RecommendationResponse> fallback = getFallbackRecommendations();
        Set<Long> existingPostIds = recommendations.stream()
                .map(RecommendationResponse::getPostId)
                .collect(Collectors.toSet());

        List<RecommendationResponse> additionalRecs = fallback.stream()
                .filter(rec -> !existingPostIds.contains(rec.getPostId()))
                .limit(FINAL_RECOMMENDATION_COUNT - recommendations.size())
                .collect(Collectors.toList());

        recommendations.addAll(additionalRecs);
    }

    // 나머지 메서드들 (상호작용, 스코어 업데이트 등)은 기존과 동일하므로 생략...
    @Transactional(readOnly = false)
    public void rebuildRecommendationsForBusiness(Member member, Business business) {
        log.info("사업장 변경으로 인한 추천 재구축: Member {}, Business {} (지역: {} {})",
                member.getEmail(), business.getBusinessId(), business.getSidoName(), business.getSigunguName());

        try {
            clearMemberCache(member.getEmail());
            cleanupObsoleteInterestScores(member, business);
            initializeBasicRecommendations(member, business);
            generateNewRecommendationsAsync(member.getEmail());
            log.info("추천 재구축 완료 (신규 지역: {} {})", business.getSidoName(), business.getSigunguName());

        } catch (Exception e) {
            log.error("추천 재구축 실패: Member {}, Business {}",
                    member.getEmail(), business.getBusinessId(), e);
        }
    }

    private void cleanupObsoleteInterestScores(Member member, Business newBusiness) {
        try {
            List<InterestScore> existingScores = interestScoreRepository
                    .findByMemberAndBusinessOrderByTotalScoreDesc(member, newBusiness);

            if (!existingScores.isEmpty()) {
                log.info("기존 관심도 스코어 정리: {}건 (지역 변경으로 인한 재계산)", existingScores.size());
                interestScoreRepository.deleteAll(existingScores);
                log.info("기존 관심도 스코어 완전 삭제 완료");
            }
        } catch (Exception e) {
            log.warn("기존 관심도 스코어 정리 실패", e);
        }
    }

    @Async
    protected void generateNewRecommendationsAsync(String email) {
        try {
            getPersonalizedRecommendations(email);
        } catch (Exception e) {
            log.error("비동기 추천 생성 실패: {}", email, e);
        }
    }

    private Business findBusinessById(Long businessId) {
        return businessRepository.findById(businessId)
                .orElseThrow(() -> new BusinessException(ResponseCode.BUSINESS_NOT_FOUND));
    }

    public void clearRecommendationsForMember(Member member) {
        log.info("회원 추천 데이터 완전 삭제: {}", member.getEmail());
        try {
            clearMemberCache(member.getEmail());
        } catch (Exception e) {
            log.error("회원 추천 데이터 삭제 실패: {}", member.getEmail(), e);
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
            clearMemberCache(email);

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
     * 단일 게시물 관심도 스코어 업데이트 (시간 감쇠 적용)
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

        // 🔥 시간 감쇠 적용한 스코어 계산
        Double viewScore = calculateViewScore(interactions);
        Double engagementScore = calculateTimeDecayedEngagementScore(interactions);
        Double timeScore = calculateTimeScore(interactions);

        // 🔥 중복 처리 로직 추가
        List<InterestScore> existingScores = interestScoreRepository
                .findAllByMemberAndBusinessAndPost(member, business, post);

        InterestScore score;
        if (existingScores.isEmpty()) {
            // 새로 생성
            score = InterestScore.builder()
                    .member(member)
                    .business(business)
                    .post(post)
                    .build();
        } else {
            // 첫 번째 것만 사용하고 나머지는 삭제
            score = existingScores.get(0);
            if (existingScores.size() > 1) {
                log.warn("중복 InterestScore 발견: Member {}, Post {}, Business {} - {}개 중복",
                        member.getEmail(), post.getPostId(), business.getBusinessId(), existingScores.size());

                // 나머지 중복 데이터 삭제
                List<InterestScore> duplicates = existingScores.subList(1, existingScores.size());
                interestScoreRepository.deleteAll(duplicates);
                log.info("중복 InterestScore 삭제 완료: {}개", duplicates.size());
            }
        }

        score.updateScore(viewScore, engagementScore, timeScore);
        interestScoreRepository.save(score);

        log.debug("시간 감쇠 적용 스코어 업데이트 완료: Post({}) - Total: {}", post.getPostId(), score.getTotalScore());
    }

    /**
     * 🆕 시간 감쇠가 적용된 참여 스코어 계산
     */
    private Double calculateTimeDecayedEngagementScore(List<UserInteraction> interactions) {
        if (interactions.isEmpty()) {
            return 0.0;
        }

        LocalDateTime now = LocalDateTime.now();
        double totalScore = 0.0;

        for (UserInteraction interaction : interactions) {
            // 1. 상호작용별 기본 점수
            double baseScore = getInteractionBaseScore(interaction);

            // 2. 시간에 따른 감쇠 계산
            long daysSince = ChronoUnit.DAYS.between(interaction.getCreatedAt(), now);
            double decayFactor = calculateDecayFactor(daysSince);

            // 3. 감쇠 적용한 점수
            double decayedScore = baseScore * decayFactor;
            totalScore += decayedScore;

            log.debug("상호작용 점수: {} -> {} ({}일 전, 감쇠율: {})",
                    baseScore, decayedScore, daysSince, String.format("%.2f", decayFactor));
        }

        return Math.max(0.0, Math.min(totalScore, 200.0));
    }

    /**
     * 🆕 상호작용별 기본 점수 계산
     */
    private double getInteractionBaseScore(UserInteraction interaction) {
        switch (interaction.getInteractionType()) {
            case VIEW -> { return 5.0; }
            case SCRAP -> { return 50.0; }
            case UNSCRAP -> { return -30.0; }  // 더 큰 페널티
            case COMMENT -> { return 30.0; }
            case CLICK_LINK -> { return 40.0; }
            case SCROLL -> { return 10.0; }  // 스크롤 점수 추가
            case RATING -> {
                if (interaction.getRating() != null) {
                    if (interaction.getRating() <= 2) {
                        return -20.0;  // 낮은 평점은 마이너스
                    } else {
                        return interaction.getRating() * 8.0;
                    }
                }
                return 0.0;
            }
            default -> { return 0.0; }
        }
    }

    /**
     * 🆕 시간에 따른 감쇠 계산
     * - 7일 미만: 100% 유지
     * - 30일 후: 80%
     * - 90일 후: 30%
     * - 180일 후: 10%
     */
    private double calculateDecayFactor(long daysSince) {
        if (daysSince < 7) {
            return 1.0;  // 최근 1주일은 감쇠 없음
        } else if (daysSince < 30) {
            // 7-30일: 선형 감소 (100% -> 80%)
            return 1.0 - (daysSince - 7) * 0.01;
        } else if (daysSince < 90) {
            // 30-90일: 선형 감소 (80% -> 30%)
            return 0.8 - (daysSince - 30) * 0.008;
        } else if (daysSince < 180) {
            // 90-180일: 선형 감소 (30% -> 10%)
            return 0.3 - (daysSince - 90) * 0.002;
        } else {
            return 0.1;  // 180일 이후는 10% 유지
        }
    }

    /**
     * 🆕 상대적 관심도 조정
     * 사용자의 다른 관심사와 비교하여 상대적 순위 조정
     */
    @Transactional(readOnly = false)
    public void adjustRelativeInterestScores(Member member) {
        Business business = businessRepository.findByMember(member).stream()
                .filter(b -> b.getStatus() == BusinessStatus.ACTIVE)
                .findFirst()
                .orElse(null);

        if (business == null) return;

        List<InterestScore> allScores = interestScoreRepository
                .findByMemberAndBusinessOrderByTotalScoreDesc(member, business);

        if (allScores.size() < 10) return;  // 충분한 데이터가 있을 때만 조정

        // 상위 20% 이하는 점수 하락
        int topThreshold = Math.max(1, allScores.size() / 5);

        for (int i = topThreshold; i < allScores.size(); i++) {
            InterestScore score = allScores.get(i);

            // 하위 80%는 점수 감소
            double decayRate = 0.95;  // 5% 감소
            if (i > allScores.size() * 0.8) {
                decayRate = 0.90;  // 하위 20%는 10% 감소
            }

            double newScore = score.getTotalScore() * decayRate;
            score.updateTotalScore(newScore);
            interestScoreRepository.save(score);
        }

        log.info("상대적 관심도 조정 완료: {}건 중 {}건 조정",
                allScores.size(), allScores.size() - topThreshold);
    }

    /**
     * 🆕 정기적 스코어 재계산 (매일 새벽 2시 실행)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional(readOnly = false)
    public void recalculateAllInterestScores() {
        log.info("전체 관심도 스코어 재계산 시작");

        List<Member> activeMembers = memberService.findAllActiveMembers();
        int totalUpdated = 0;

        for (Member member : activeMembers) {
            try {
                Business business = businessRepository.findByMember(member).stream()
                        .filter(b -> b.getStatus() == BusinessStatus.ACTIVE)
                        .findFirst()
                        .orElse(null);

                if (business == null) continue;

                List<InterestScore> scores = interestScoreRepository
                        .findByMemberAndBusiness(member, business);

                for (InterestScore score : scores) {
                    // 해당 게시물에 대한 모든 상호작용 다시 조회
                    List<UserInteraction> interactions = userInteractionRepository
                            .findByMemberAndPost(member, score.getPost());

                    // 시간 감쇠 적용한 새로운 점수 계산
                    Double newEngagementScore = calculateTimeDecayedEngagementScore(interactions);
                    Double viewScore = calculateViewScore(interactions);
                    Double timeScore = calculateTimeScore(interactions);

                    // 점수 업데이트
                    score.updateScore(viewScore, newEngagementScore, timeScore);
                    interestScoreRepository.save(score);
                    totalUpdated++;
                }

                // 상대적 조정도 함께 실행
                adjustRelativeInterestScores(member);

                // 캐시 클리어
                clearMemberCache(member.getEmail());

            } catch (Exception e) {
                log.error("회원 스코어 재계산 실패: {}", member.getEmail(), e);
            }
        }

        log.info("전체 관심도 스코어 재계산 완료: {}건 업데이트", totalUpdated);
    }

    /**
     * 조회 기반 스코어 계산 (기존 유지)
     */
    private Double calculateViewScore(List<UserInteraction> interactions) {
        long viewCount = interactions.stream()
                .filter(i -> i.getInteractionType() == InteractionType.VIEW)
                .count();
        return Math.min(viewCount * 10.0, 100.0);
    }

    /**
     * 체류시간 기반 스코어 계산 (기존 유지)
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
}
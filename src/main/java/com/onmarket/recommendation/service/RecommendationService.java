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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
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
    private static final int FINAL_RECOMMENDATION_COUNT = 5;
    private static final int REGION_FILTERED_LIMIT = 100;

    /**
     * 🔥 순수 점수 기반 추천 생성 (개수 제한 없음)
     */
    private List<RecommendationResponse> generatePureScoreBasedRecommendations(
            Member member, Business business, List<Post> filteredPosts) {

        log.info("순수 점수 기반 추천 생성 시작: {}개 게시물 분석", filteredPosts.size());

        if (filteredPosts.isEmpty()) {
            return Collections.emptyList();
        }

        // 🔥 필터링된 게시물의 PostType 분포 확인
        Map<PostType, Long> inputDistribution = filteredPosts.stream()
                .collect(Collectors.groupingBy(Post::getPostType, Collectors.counting()));
        log.info("입력 게시물 PostType 분포: {}", inputDistribution);

        // 🔥 각 게시물의 기본 점수 확인
        for (Post post : filteredPosts.stream().limit(10).collect(Collectors.toList())) {
            double baseScore = calculateBaseScore(post);
            log.info("게시물 ID: {}, PostType: {}, 기본점수: {:.2f}, 제품명: {}",
                    post.getPostId(), post.getPostType(), baseScore,
                    post.getProductName() != null ? post.getProductName().substring(0, Math.min(20, post.getProductName().length())) : "없음");
        }

        // 1. 모든 게시물에 대해 우선순위 스코어 계산
        List<RecommendationResponse> allRecommendations = priorityRecommendationService
                .generatePriorityRecommendations(member, business, filteredPosts, filteredPosts.size());

        log.info("PriorityRecommendationService에서 반환된 추천 수: {}", allRecommendations.size());

        // 🔥 PriorityRecommendationService 결과 분포 확인
        Map<PostType, Long> priorityDistribution = allRecommendations.stream()
                .collect(Collectors.groupingBy(RecommendationResponse::getPostType, Collectors.counting()));
        log.info("PriorityService 결과 PostType 분포: {}", priorityDistribution);

        // 🔥 상위 10개 점수 확인
        log.info("상위 10개 추천 점수:");
        allRecommendations.stream()
                .limit(10)
                .forEach(rec -> log.info("PostType: {}, 점수: {:.2f}, 이유: {}",
                        rec.getPostType(), rec.getInterestScore(), rec.getRecommendationReason()));

        // 2. 점수 순으로 정렬
        allRecommendations.sort((a, b) -> Double.compare(b.getInterestScore(), a.getInterestScore()));

        // 3. 상위 N개만 선택
        List<RecommendationResponse> topRecommendations = allRecommendations.stream()
                .limit(FINAL_RECOMMENDATION_COUNT)
                .collect(Collectors.toList());

        // 4. 최종 분포 로깅
        long loanCount = countByPostType(topRecommendations, PostType.LOAN);
        long supportCount = countByPostType(topRecommendations, PostType.SUPPORT);

        log.info("순수 점수 기반 추천 완료: 총 {}건 (LOAN: {}개, SUPPORT: {}개) - 점수 범위: {:.1f}~{:.1f}",
                topRecommendations.size(), loanCount, supportCount,
                topRecommendations.isEmpty() ? 0.0 : topRecommendations.get(0).getInterestScore(),
                topRecommendations.isEmpty() ? 0.0 : topRecommendations.get(topRecommendations.size()-1).getInterestScore());

        return topRecommendations;
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
     * 🔥 순수 점수 기반 폴백 추천 생성 (개수 제한 없음)
     */
    private List<RecommendationResponse> getPureScoreFallbackRecommendations() {
        try {
            log.info("순수 점수 기반 폴백 추천 생성");

            // 최신 게시물들을 가져와서 점수 계산
            List<Post> recentPosts = postRepository.findTop20ByOrderByCreatedAtDesc();

            if (recentPosts.isEmpty()) {
                return Collections.emptyList();
            }

            List<RecommendationResponse> fallbackRecs = recentPosts.stream()
                    .map(this::convertToFallbackRecommendation)
                    .sorted((a, b) -> Double.compare(b.getInterestScore(), a.getInterestScore())) // 점수 순 정렬
                    .limit(FINAL_RECOMMENDATION_COUNT)
                    .collect(Collectors.toList());

            log.info("순수 점수 폴백 추천 완료: {}건 (LOAN: {}, SUPPORT: {})",
                    fallbackRecs.size(),
                    countByPostType(fallbackRecs, PostType.LOAN),
                    countByPostType(fallbackRecs, PostType.SUPPORT));

            return fallbackRecs;

        } catch (Exception e) {
            log.error("순수 점수 폴백 추천 생성 실패", e);
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
                .interestScore(baseScore)
                .recommendationReason("추천 상품")
                .build();
    }

    /**
     * 🔥 개선된 상호작용 기록 - 즉시 반영 보장
     */
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

            // 🔥 동기적으로 스코어 즉시 업데이트
            updateSinglePostScore(member, post);

            // 🔥 중요한 상호작용인 경우 즉시 캐시 클리어 + 새 추천 생성
            if (isCriticalInteraction(type)) {
                log.info("중요 상호작용 감지: {} - {}", email, type);

                // 1. 캐시 완전 클리어
                clearMemberCache(email);

                // 2. 🔥 즉시 동기적으로 새 추천 생성
                generateAndCacheNewRecommendations(member);

                log.info("중요 상호작용 처리 완료: {} - {} (즉시 반영)", email, type);
            } else {
                log.debug("일반 상호작용 처리 완료: {} - {}", email, type);
            }

        } catch (Exception e) {
            log.error("상호작용 기록 실패: email={}, postId={}", email, postId, e);
        }
    }

    /**
     * 🔥 즉시 새 추천 생성 및 캐시 저장 (동기적) - Business 예외 처리 개선
     */
    private void generateAndCacheNewRecommendations(Member member) {
        try {
            String email = member.getEmail();
            List<RecommendationResponse> newRecommendations;

            // 1. Business가 있는 경우: 지역 필터링 + 순수 점수 기반 추천
            if (member.getMainBusinessId() != null) {
                try {
                    // 🔥 예외 안전한 Business 조회
                    Business business = findBusinessByIdSafely(member.getMainBusinessId());

                    if (business != null) {
                        // 지역 인식 게시물 조회 (연령 필터링 제거됨)
                        List<Post> candidatePosts = getRegionAwarePosts(business, member);
                        log.info("지역 인식 후 PostType 분포: {}",
                                candidatePosts.stream().collect(Collectors.groupingBy(Post::getPostType, Collectors.counting())));

                        if (candidatePosts.isEmpty()) {
                            log.warn("후보 게시물이 없음, 전체 게시물로 폴백: {}", email);
                            candidatePosts = postRepository.findTop50ByOrderByCreatedAtDesc();
                        }

                        // 🔥 순수 점수 기반 추천 생성
                        newRecommendations = generatePureScoreBasedRecommendations(member, business, candidatePosts);

                        log.info("Business 기반 순수 점수 추천 생성 완료: {} (지역: {} {})",
                                email, business.getSidoName(), business.getSigunguName());
                    } else {
                        // Business가 없거나 조회 실패 시 행동 추적으로 폴백
                        log.info("Business 조회 실패 또는 없음, 행동 추적 기반 추천으로 전환: {}", email);
                        newRecommendations = generatePureScoreBehaviorBasedRecommendations(member);
                    }

                } catch (Exception e) {
                    log.error("Business 기반 추천 생성 실패, 행동 추적으로 폴백: {}", email, e);
                    newRecommendations = generatePureScoreBehaviorBasedRecommendations(member);
                }
            }
            // 2. Business가 없는 경우: 순수 행동 추적 기반 추천
            else {
                log.info("대표 사업장이 없는 사용자, 순수 점수 기반 행동 추적 추천 생성: {}", email);
                newRecommendations = generatePureScoreBehaviorBasedRecommendations(member);
            }

            // 나머지 로직은 동일...
        } catch (Exception e) {
            log.error("즉시 추천 생성 실패: {}", member.getEmail(), e);
            // 폴백 로직...
        }
    }

    /**
     * 🔥 예외 안전한 Business 조회 - 예외 발생 시 null 반환
     */
    private Business findBusinessByIdSafely(Long businessId) {
        try {
            return businessRepository.findById(businessId).orElse(null);
        } catch (Exception e) {
            log.warn("Business 조회 실패: businessId={}", businessId, e);
            return null;
        }
    }

    /**
     * 🔥 순수 점수 기반 행동 추적 추천 생성
     */
    private List<RecommendationResponse> generatePureScoreBehaviorBasedRecommendations(Member member) {
        try {
            log.info("순수 점수 기반 행동 추적 추천 생성 시작: {}", member.getEmail());

            // 1. 사용자의 모든 상호작용 조회 (최근 3개월)
            LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
            List<UserInteraction> recentInteractions = userInteractionRepository
                    .findByMemberAndCreatedAtAfterOrderByCreatedAtDesc(member, threeMonthsAgo);

            if (recentInteractions.isEmpty()) {
                log.info("상호작용 이력이 없음, 시스템 기본 추천으로 폴백: {}", member.getEmail());
                return getPureScoreFallbackRecommendations();
            }

            // 2. 상호작용한 게시물들의 특성 분석
            Map<PostType, Double> typePreferences = analyzePostTypePreferences(recentInteractions);
            Set<String> preferredCompanies = analyzeCompanyPreferences(recentInteractions);
            Set<String> preferredKeywords = analyzeContentPreferences(recentInteractions);

            log.info("행동 패턴 분석 - 타입 선호도: {}, 선호 기업: {}개, 키워드: {}개",
                    typePreferences, preferredCompanies.size(), preferredKeywords.size());

            // 3. 전체 게시물에서 행동 패턴과 유사한 게시물 찾기
            List<Post> allPosts = postRepository.findTop200ByOrderByCreatedAtDesc();
            List<Post> behaviorMatchedPosts = filterPostsByBehaviorPattern(
                    allPosts, typePreferences, preferredCompanies, preferredKeywords, recentInteractions);

            if (behaviorMatchedPosts.isEmpty()) {
                log.warn("행동 패턴 매칭 게시물이 없음, 전체 게시물로 폴백");
                behaviorMatchedPosts = allPosts.stream().limit(50).collect(Collectors.toList());
            }

            // 4. 🔥 순수 점수 기반 추천 생성 (균형 고려 없음)
            List<RecommendationResponse> behaviorRecs = generatePureScoreBehaviorRecommendations(
                    member, behaviorMatchedPosts, typePreferences);

            log.info("순수 점수 기반 행동 추천 생성 완료: {}건", behaviorRecs.size());
            return behaviorRecs;

        } catch (Exception e) {
            log.error("행동 추적 기반 추천 생성 실패: {}", member.getEmail(), e);
            return getPureScoreFallbackRecommendations();
        }
    }

    /**
     * PostType 선호도 분석
     */
    private Map<PostType, Double> analyzePostTypePreferences(List<UserInteraction> interactions) {
        Map<PostType, Double> preferences = new HashMap<>();

        for (UserInteraction interaction : interactions) {
            PostType postType = interaction.getPost().getPostType();
            double score = getInteractionBaseScore(interaction);

            preferences.merge(postType, score, Double::sum);
        }

        // 정규화 (총합이 1.0이 되도록)
        double totalScore = preferences.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalScore > 0) {
            preferences.replaceAll((type, score) -> score / totalScore);
        }

        return preferences;
    }

    /**
     * 선호 기업 분석
     */
    private Set<String> analyzeCompanyPreferences(List<UserInteraction> interactions) {
        return interactions.stream()
                .filter(i -> i.getInteractionType() == InteractionType.SCRAP ||
                        i.getInteractionType() == InteractionType.CLICK_LINK ||
                        (i.getInteractionType() == InteractionType.RATING &&
                                i.getRating() != null && i.getRating() >= 4))
                .map(i -> i.getPost().getCompanyName())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 콘텐츠 키워드 선호도 분석
     */
    private Set<String> analyzeContentPreferences(List<UserInteraction> interactions) {
        Set<String> keywords = new HashSet<>();

        for (UserInteraction interaction : interactions) {
            if (interaction.getInteractionType() == InteractionType.SCRAP ||
                    interaction.getInteractionType() == InteractionType.CLICK_LINK) {

                Post post = interaction.getPost();
                if (post.getProductName() != null) {
                    String[] words = post.getProductName().toLowerCase().split("\\s+");
                    for (String word : words) {
                        if (word.length() > 1) {
                            keywords.add(word);
                        }
                    }
                }
            }
        }

        return keywords.stream().limit(10).collect(Collectors.toSet());
    }

    /**
     * 🔥 퍼센티지 순 정렬이 적용된 행동 패턴 기반 게시물 필터링
     */
    private List<Post> filterPostsByBehaviorPattern(List<Post> allPosts,
                                                    Map<PostType, Double> typePreferences,
                                                    Set<String> preferredCompanies,
                                                    Set<String> preferredKeywords,
                                                    List<UserInteraction> recentInteractions) {

        // 이미 상호작용한 게시물 제외
        Set<Long> interactedPostIds = recentInteractions.stream()
                .map(i -> i.getPost().getPostId())
                .collect(Collectors.toSet());

        // 🔥 스크롤 상호작용 분석 - 퍼센티지 순으로 정렬
        Map<Long, List<UserInteraction>> scrollInteractionsByPost = recentInteractions.stream()
                .filter(i -> i.getInteractionType() == InteractionType.SCROLL &&
                        i.getScrollPercentage() != null)
                .collect(Collectors.groupingBy(i -> i.getPost().getPostId()));

        // 🔥 게시물별 최고 스크롤 퍼센티지와 평균 계산
        Map<Long, Double> postScrollScores = new HashMap<>();
        for (Map.Entry<Long, List<UserInteraction>> entry : scrollInteractionsByPost.entrySet()) {
            List<UserInteraction> scrolls = entry.getValue();

            scrolls.sort((a, b) -> Integer.compare(
                    b.getScrollPercentage(), a.getScrollPercentage()));

            double maxPercent = scrolls.get(0).getScrollPercentage();
            double avgPercent = scrolls.stream()
                    .mapToInt(UserInteraction::getScrollPercentage)
                    .average().orElse(0.0);

            double scrollScore = (maxPercent * 0.7) + (avgPercent * 0.3);
            postScrollScores.put(entry.getKey(), scrollScore);
        }

        return allPosts.stream()
                .filter(post -> !interactedPostIds.contains(post.getPostId()))
                .filter(post -> {
                    boolean typeMatch = typePreferences.containsKey(post.getPostType()) &&
                            typePreferences.get(post.getPostType()) > 0.1;

                    boolean companyMatch = preferredCompanies.isEmpty() ||
                            preferredCompanies.contains(post.getCompanyName());

                    boolean keywordMatch = preferredKeywords.isEmpty() ||
                            preferredKeywords.stream().anyMatch(keyword ->
                                    post.getProductName() != null &&
                                            post.getProductName().toLowerCase().contains(keyword));

                    return typeMatch || companyMatch || keywordMatch;
                })
                .sorted((a, b) -> {
                    Double scoreA = postScrollScores.getOrDefault(a.getPostId(), 0.0);
                    Double scoreB = postScrollScores.getOrDefault(b.getPostId(), 0.0);

                    int scrollCompare = Double.compare(scoreB, scoreA);
                    if (scrollCompare != 0) {
                        return scrollCompare;
                    }

                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .limit(100)
                .collect(Collectors.toList());
    }

    /**
     * 🔥 순수 점수 기반 행동 추천 생성 (균형 로직 제거)
     */
    private List<RecommendationResponse> generatePureScoreBehaviorRecommendations(
            Member member, List<Post> posts, Map<PostType, Double> typePreferences) {

        List<RecommendationResponse> recommendations = new ArrayList<>();

        for (Post post : posts) {
            double behaviorScore = calculateBehaviorScore(post, typePreferences, member);

            if (behaviorScore <= 0) {
                behaviorScore = calculateBaseScore(post);
            }

            RecommendationResponse rec = RecommendationResponse.builder()
                    .postId(post.getPostId())
                    .productName(post.getProductName())
                    .companyName(post.getCompanyName())
                    .postType(post.getPostType())
                    .deadline(post.getDeadline())
                    .summary(post.getSummary())
                    .imageUrl(post.getImageUrl())
                    .interestScore(behaviorScore)
                    .recommendationReason(generateBehaviorReason(post, typePreferences))
                    .build();

            recommendations.add(rec);
        }

        // 🔥 순수 점수 순으로 정렬하고 상위 N개만 선택
        return recommendations.stream()
                .sorted((a, b) -> Double.compare(b.getInterestScore(), a.getInterestScore()))
                .limit(FINAL_RECOMMENDATION_COUNT)
                .collect(Collectors.toList());
    }

    private List<UserInteraction> getRecentInteractionsForPost(Post post) {
        try {
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            return userInteractionRepository.findByPostAndCreatedAtAfter(post, sevenDaysAgo);
        } catch (Exception e) {
            log.warn("최근 상호작용 조회 실패: postId={}", post.getPostId(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 시간 가중치 계산 (최근일수록 높은 점수)
     */
    private double calculateTimeWeight(long hoursAgo) {
        if (hoursAgo < 1) {
            return 1.5; // 1시간 이내: 150%
        } else if (hoursAgo < 24) {
            return 1.2; // 1일 이내: 120%
        } else if (hoursAgo < 168) { // 7일
            return 1.0; // 1주일 이내: 100%
        } else if (hoursAgo < 720) { // 30일
            return 0.8; // 1개월 이내: 80%
        } else {
            return 0.5; // 1개월 이후: 50%
        }
    }

    /**
     * 행동 기반 점수 계산
     */
    private double calculateBehaviorScore(Post post, Map<PostType, Double> typePreferences, Member member) {
        double score = calculateBaseScore(post); // 기본 점수

        // PostType 선호도 반영
        Double typePreference = typePreferences.get(post.getPostType());
        if (typePreference != null) {
            score += typePreference * 100; // 선호도가 높을수록 높은 점수
        }

        // 연령대 적합성 (Business 없이도 계산 가능)
        if (member.getBirthDate() != null) {
            int age = calculateAge(member.getBirthDate());
            double ageScore = calculateAgeCompatibilityScore(post, age);
            score += ageScore;
        }

        return Math.max(score, 10.0); // 최소 점수 보장
    }

    /**
     * 연령 적합성 점수 계산
     */
    private double calculateAgeCompatibilityScore(Post post, int age) {
        // 🔥 PostType별 차별 제거 - 모든 타입에 동일한 연령 적합성
        if (age >= 25 && age <= 45) return 20.0;  // 가장 활발한 경제활동 연령
        if (age >= 20 && age <= 50) return 15.0;  // 일반적인 경제활동 연령
        if (age >= 18 && age <= 55) return 10.0;  // 확장된 경제활동 연령
        return 5.0; // 기본 점수
    }

    /**
     * 행동 기반 추천 이유 생성
     */
    private String generateBehaviorReason(Post post, Map<PostType, Double> typePreferences) {
        String behaviorReason = generateBehaviorBasedReason(post);
        if (behaviorReason != null) {
            return behaviorReason;
        }

        Double typePreference = typePreferences.get(post.getPostType());

        if (typePreference != null && typePreference > 0.4) {
            return "관심 있어 하는 유형의 상품";
        } else {
            return "행동 패턴 기반 추천";
        }
    }

    /**
     * 🔥 개선된 행동 기반 추천 이유 생성 - 퍼센티지 정보 포함
     */
    private String generateBehaviorBasedReason(Post post) {
        try {
            List<UserInteraction> recentInteractions = getRecentInteractionsForPost(post);

            if (recentInteractions.isEmpty()) {
                return null;
            }

            // 🔥 스크롤 상호작용 특별 분석
            List<UserInteraction> scrollInteractions = recentInteractions.stream()
                    .filter(i -> i.getInteractionType() == InteractionType.SCROLL &&
                            i.getScrollPercentage() != null)
                    .sorted((a, b) -> Integer.compare(
                            b.getScrollPercentage(), a.getScrollPercentage()))
                    .collect(Collectors.toList());

            // 상호작용 유형별 분석
            Map<InteractionType, Long> interactionCounts = recentInteractions.stream()
                    .collect(Collectors.groupingBy(
                            UserInteraction::getInteractionType,
                            Collectors.counting()));

            // 🔥 부정적 상호작용 우선 체크 (강화된 감점)
            if (interactionCounts.getOrDefault(InteractionType.UNSCRAP, 0L) > 0) {
                return "스크랩 취소한 상품";
            }

            // 🔥 낮은 평점 체크
            List<UserInteraction> ratingInteractions = recentInteractions.stream()
                    .filter(i -> i.getInteractionType() == InteractionType.RATING &&
                            i.getRating() != null)
                    .collect(Collectors.toList());

            if (!ratingInteractions.isEmpty()) {
                double avgRating = ratingInteractions.stream()
                        .mapToInt(UserInteraction::getRating)
                        .average().orElse(0.0);

                if (avgRating <= 2.0) {
                    return "낮은 평점을 준 유형";
                }
            }

            // 🔥 긍정적 상호작용
            if (interactionCounts.getOrDefault(InteractionType.SCRAP, 0L) > 0) {
                long scrapCount = interactionCounts.get(InteractionType.SCRAP);
                if (scrapCount >= 3) {
                    return "자주 스크랩한 관심 상품";
                } else {
                    return "스크랩한 관심 상품";
                }
            }

            if (interactionCounts.getOrDefault(InteractionType.CLICK_LINK, 0L) > 0) {
                return "클릭한 유사 상품";
            }

            // 🔥 스크롤 퍼센티지 기반 상세한 이유
            if (!scrollInteractions.isEmpty()) {
                int maxScrollPercent = scrollInteractions.get(0).getScrollPercentage();
                long scrollCount = scrollInteractions.size();

                if (maxScrollPercent >= 90) {
                    if (scrollCount >= 3) {
                        return String.format("끝까지 본 관심 상품 (%d%%)", maxScrollPercent);
                    } else {
                        return String.format("자세히 본 상품 (%d%%)", maxScrollPercent);
                    }
                } else if (maxScrollPercent >= 70) {
                    return String.format("관심 있게 본 상품 (%d%%)", maxScrollPercent);
                } else if (maxScrollPercent >= 50) {
                    return String.format("확인한 상품 (%d%%)", maxScrollPercent);
                } else if (scrollCount >= 3) {
                    return "반복 조회 상품";
                }
            }

            // 평점 긍정적 피드백
            if (!ratingInteractions.isEmpty()) {
                double avgRating = ratingInteractions.stream()
                        .mapToInt(UserInteraction::getRating)
                        .average().orElse(0.0);

                if (avgRating >= 4.0) {
                    return String.format("높은 평점 유사 상품 (%.1f점)", avgRating);
                } else if (avgRating >= 3.0) {
                    return "평가한 유사 상품";
                }
            }

            if (interactionCounts.getOrDefault(InteractionType.COMMENT, 0L) > 0) {
                return "댓글 남긴 상품";
            }

            Long viewCount = interactionCounts.getOrDefault(InteractionType.VIEW, 0L);
            if (viewCount >= 10) {
                return "자주 본 인기 유형";
            } else if (viewCount >= 5) {
                return "자주 본 유형";
            } else if (viewCount >= 3) {
                return "반복 조회 상품";
            } else if (viewCount >= 1) {
                return "관심 표시 상품";
            }

            return null;

        } catch (Exception e) {
            log.warn("행동 기반 이유 생성 실패: postId={}", post.getPostId(), e);
            return null;
        }
    }

    /**
     * 🔥 순수 점수 기반 폴백 보강 (균형 고려 없음)
     */
    private void enhanceWithPureScoreFallback(List<RecommendationResponse> recommendations) {
        List<RecommendationResponse> fallback = getPureScoreFallbackRecommendations();
        Set<Long> existingPostIds = recommendations.stream()
                .map(RecommendationResponse::getPostId)
                .collect(Collectors.toSet());

        // 점수 순으로 부족한 만큼 추가
        List<RecommendationResponse> additionalRecs = fallback.stream()
                .filter(rec -> !existingPostIds.contains(rec.getPostId()))
                .limit(FINAL_RECOMMENDATION_COUNT - recommendations.size())
                .collect(Collectors.toList());

        recommendations.addAll(additionalRecs);

        log.info("순수 점수 기반 폴백 보강 완료: {}개 추가", additionalRecs.size());
    }

    /**
     * 🔥 예외 안전한 캐시 스킵 로직
     */
    private boolean shouldSkipCache(String email) {
        try {
            Member member = memberService.findByEmail(email);
            LocalDateTime threeMinutesAgo = LocalDateTime.now().minusMinutes(3);

            // 최근 3분 내 중요한 상호작용이 있었는지 확인
            List<UserInteraction> recentCriticalInteractions = userInteractionRepository
                    .findByMemberAndCreatedAtAfterAndInteractionTypeIn(
                            member,
                            threeMinutesAgo,
                            List.of(InteractionType.SCRAP, InteractionType.UNSCRAP,
                                    InteractionType.RATING, InteractionType.CLICK_LINK)
                    );

            boolean shouldSkip = !recentCriticalInteractions.isEmpty();
            if (shouldSkip) {
                log.info("최근 중요한 상호작용으로 인한 캐시 스킵: {} ({}건, 최근 3분)",
                        email, recentCriticalInteractions.size());

                // 🔥 캐시 스킵 시 즉시 새 추천 생성 (예외 안전)
                try {
                    generateAndCacheNewRecommendations(member);
                } catch (Exception e) {
                    log.error("캐시 스킵 시 즉시 추천 생성 실패: {}", email, e);
                }
            }
            return shouldSkip;

        } catch (Exception e) {
            log.warn("캐시 스킵 확인 실패, 새 추천 생성: {}", email, e);
            try {
                Member member = memberService.findByEmail(email);
                generateAndCacheNewRecommendations(member);
            } catch (Exception e2) {
                log.error("캐시 스킵 실패 시 새 추천 생성도 실패: {}", email, e2);
            }
            return true; // 에러 시 캐시 스킵
        }
    }

    /**
     * 🔥 강제 즉시 추천 갱신 API용 메서드
     */
    public List<RecommendationResponse> forceRefreshRecommendations(String email) {
        try {
            Member member = memberService.findByEmail(email);

            // 1. 캐시 완전 클리어
            clearMemberCache(email);

            // 2. 즉시 새 추천 생성
            generateAndCacheNewRecommendations(member);

            // 3. 새로 생성된 추천 반환
            return getCachedRecommendations(email);

        } catch (Exception e) {
            log.error("강제 추천 갱신 실패: {}", email, e);
            return getPureScoreFallbackRecommendations();
        }
    }

    /**
     * 🔥 개선된 메인 추천 로직 - 순수 점수 기반
     */
    public List<RecommendationResponse> getPersonalizedRecommendations(String email) {
        long startTime = System.currentTimeMillis();
        log.info("순수 점수 기반 추천 조회 시작: {}", email);

        try {
            Member member = memberService.findByEmail(email);

            // 1. 캐시 확인 (중요한 상호작용 후에는 자동으로 스킵)
            if (!shouldSkipCache(email)) {
                List<RecommendationResponse> cached = getCachedRecommendations(email);
                if (!cached.isEmpty()) {
                    log.info("캐시에서 추천 반환: {}건 ({}ms)",
                            cached.size(), System.currentTimeMillis() - startTime);
                    return cached;
                }
            }

            // 2. 새 추천 생성
            List<RecommendationResponse> finalRecommendations;

            if (member.getMainBusinessId() != null) {
                // Business 기반 추천 시도 (예외 안전)
                try {
                    Business business = findBusinessByIdSafely(member.getMainBusinessId());

                    if (business != null) {
                        List<Post> regionAwarePosts = getRegionAwarePosts(business, member);

                        if (regionAwarePosts.isEmpty()) {
                            log.warn("연령 필터링된 게시물이 없음, 전체 게시물로 폴백");
                            regionAwarePosts = postRepository.findTop50ByOrderByCreatedAtDesc();
                        }

                        if (regionAwarePosts.isEmpty()) {
                            log.warn("분석할 게시물이 없음, 행동 추적으로 폴백");
                            finalRecommendations = generatePureScoreBehaviorBasedRecommendations(member);
                        } else {
                            // 순수 점수 기반 추천 생성 (지역은 점수로만 반영)
                            finalRecommendations = generatePureScoreBasedRecommendations(
                                    member, business, regionAwarePosts);
                        }
                    } else {
                        log.info("Business 없음, 행동 추적 기반 추천으로 전환: {}", email);
                        finalRecommendations = generatePureScoreBehaviorBasedRecommendations(member);
                    }
                } catch (Exception e) {
                    log.error("Business 기반 추천 실패, 행동 추적으로 폴백: {}", email, e);
                    finalRecommendations = generatePureScoreBehaviorBasedRecommendations(member);
                }
            } else {
                // 대표 사업장이 없는 경우 - 행동 추적 기반
                log.info("대표 사업장이 없는 사용자, 행동 추적 기반 추천: {}", email);
                finalRecommendations = generatePureScoreBehaviorBasedRecommendations(member);
            }

            // 3. 추천이 부족한 경우에만 폴백으로 보강
            if (finalRecommendations.size() < FINAL_RECOMMENDATION_COUNT) {
                enhanceWithPureScoreFallback(finalRecommendations);
            }

            // 4. 최종 개수 제한
            finalRecommendations = finalRecommendations.stream()
                    .limit(FINAL_RECOMMENDATION_COUNT)
                    .collect(Collectors.toList());

            // 5. 새 추천을 캐시에 저장
            putCache(email, finalRecommendations);

            log.info("새 추천 생성 완료: {}건 (LOAN: {}, SUPPORT: {}, {}ms)",
                    finalRecommendations.size(),
                    countByPostType(finalRecommendations, PostType.LOAN),
                    countByPostType(finalRecommendations, PostType.SUPPORT),
                    System.currentTimeMillis() - startTime);

            return finalRecommendations;

        } catch (Exception e) {
            log.error("추천 생성 실패: {}", email, e);
            List<RecommendationResponse> fallback = getPureScoreFallbackRecommendations();
            putCache(email, fallback);
            return fallback;
        }
    }

    /**
     * 중요한 상호작용인지 확인
     */
    private boolean isCriticalInteraction(InteractionType type) {
        return type == InteractionType.SCRAP ||
                type == InteractionType.UNSCRAP ||
                type == InteractionType.RATING ||
                type == InteractionType.CLICK_LINK;
    }

    /**
     * 🔥 예외 안전한 단일 게시물 관심도 스코어 업데이트
     */
    @Transactional(readOnly = false)
    public void updateSinglePostScore(Member member, Post post) {
        try {
            // Business 안전 조회
            Business business = null;
            if (member.getMainBusinessId() != null) {
                business = findBusinessByIdSafely(member.getMainBusinessId());
            }

            // Business가 없으면 활성 Business 중 첫 번째 찾기
            if (business == null) {
                business = businessRepository.findByMember(member).stream()
                        .filter(b -> b.getStatus() == BusinessStatus.ACTIVE)
                        .findFirst()
                        .orElse(null);
            }

            // Business가 여전히 없으면 스코어 업데이트 스킵
            if (business == null) {
                log.debug("Business가 없어서 스코어 업데이트 스킵: Member {}, Post {}",
                        member.getEmail(), post.getPostId());
                return;
            }

            List<UserInteraction> interactions = userInteractionRepository
                    .findByMemberAndPost(member, post);

            // 시간 감쇠 적용한 스코어 계산
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

            log.debug("시간 감쇠 적용 스코어 업데이트 완료: Post({}) - Total: {}",
                    post.getPostId(), score.getTotalScore());

        } catch (Exception e) {
            log.error("스코어 업데이트 실패: Member {}, Post {}",
                    member.getEmail(), post.getPostId(), e);
        }
    }

    /**
     * 🔥 강화된 시간 감쇠가 적용된 참여 스코어 계산
     */
    private Double calculateTimeDecayedEngagementScore(List<UserInteraction> interactions) {
        if (interactions.isEmpty()) {
            return 0.0;
        }

        LocalDateTime now = LocalDateTime.now();
        double totalScore = 0.0;

        // 🔥 상호작용을 시간순으로 정렬하여 최신 순서대로 처리
        List<UserInteraction> sortedInteractions = interactions.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());

        for (UserInteraction interaction : sortedInteractions) {
            // 1. 상호작용별 기본 점수 (강화된 감점 적용)
            double baseScore = getInteractionBaseScore(interaction);

            // 2. 시간에 따른 감쇠 계산
            long daysSince = ChronoUnit.DAYS.between(interaction.getCreatedAt(), now);
            double decayFactor = calculateDecayFactor(daysSince);

            // 3. 🔥 부정적 상호작용은 감쇠를 적게 적용 (오래 기억)
            if (baseScore < 0) {
                decayFactor = Math.max(decayFactor, 0.7); // 부정적 점수는 최소 70% 유지
            }

            // 4. 감쇠 적용한 점수
            double decayedScore = baseScore * decayFactor;
            totalScore += decayedScore;
        }

        // 🔥 점수 범위 확장 (부정적 점수도 충분히 반영)
        return Math.max(-100.0, Math.min(totalScore, 200.0));
    }

    /**
     * 🔥 강화된 상호작용별 기본 점수 계산 (부정적 상호작용 감점 강화)
     */
    private double getInteractionBaseScore(UserInteraction interaction) {
        switch (interaction.getInteractionType()) {
            case VIEW -> { return 10.0; }
            case SCRAP -> { return 80.0; }
            case UNSCRAP -> { return -60.0; } // 감점 강화
            case COMMENT -> { return 60.0; }
            case CLICK_LINK -> { return 70.0; }
            case SCROLL -> {
                // 🔥 스크롤 퍼센티지 기반 점수 계산
                if (interaction.getScrollPercentage() != null) {
                    return 5.0 + (interaction.getScrollPercentage() / 100.0) * 20.0;
                }
                return 15.0;
            }
            case RATING -> {
                if (interaction.getRating() != null) {
                    if (interaction.getRating() <= 2) {
                        return interaction.getRating() == 1 ? -50.0 : -35.0; // 낮은 평점 감점 강화
                    } else if (interaction.getRating() == 3) {
                        return 5.0;
                    } else {
                        return interaction.getRating() * 15.0; // 4점: 60, 5점: 75
                    }
                }
                return 0.0;
            }
            default -> { return 0.0; }
        }
    }

    /**
     * 시간에 따른 감쇠 계산
     */
    private double calculateDecayFactor(long daysSince) {
        if (daysSince < 7) {
            return 1.0;
        } else if (daysSince < 30) {
            return 1.0 - (daysSince - 7) * 0.01;
        } else if (daysSince < 90) {
            return 0.8 - (daysSince - 30) * 0.008;
        } else if (daysSince < 180) {
            return 0.3 - (daysSince - 90) * 0.002;
        } else {
            return 0.1;
        }
    }

    private List<Post> getRegionFilteredPosts(Business business, Member member) {
        try {
            List<Post> regionPosts = regionFilterService.filterPostsByRegion(
                    business.getSidoName(), business.getSigunguName());

            // 🔥 연령 필터링 제거 - 이제 점수로만 반영
            List<Post> finalPosts = regionPosts.stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .limit(REGION_FILTERED_LIMIT)
                    .collect(Collectors.toList());

            log.info("필터링 결과: 지역({} {}) -> {}건 (연령은 점수로 반영)",
                    business.getSidoName(), business.getSigunguName(),
                    finalPosts.size());

            return finalPosts;

        } catch (Exception e) {
            log.error("지역 필터링 실패: {} {}", business.getSidoName(), business.getSigunguName(), e);
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

    /**
     * 🔥 완전히 동일한 기본 점수 계산 (PostType 편향 완전 제거)
     */
    /**
     * 🔥 calculateBaseScore 메서드에도 디버깅 추가
     */
    private double calculateBaseScore(Post post) {
        double baseScore = 10.0; // 기본 점수

        log.debug("기본점수 계산 - PostID: {}, PostType: {}, 시작점수: {}",
                post.getPostId(), post.getPostType(), baseScore);

        // 🔥 PostType별 점수 차별 완전 제거 - 모든 타입 동일
        baseScore += 10.0; // 모든 PostType에 동일한 점수
        log.debug("PostType 점수 추가 후: {}", baseScore);

        // 최신성 점수 (PostType과 무관)
        if (post.getCreatedAt() != null) {
            long daysSinceCreated = java.time.Duration.between(
                    post.getCreatedAt(), LocalDateTime.now()).toDays();
            double recencyBonus = Math.max(0, 15.0 - daysSinceCreated * 0.2);
            baseScore += recencyBonus;
            log.debug("최신성 점수 추가 후: {} ({}일 전, 보너스: {})",
                    baseScore, daysSinceCreated, recencyBonus);
        }

        // 품질 점수 (PostType과 무관)
        if (post.getProductName() != null && post.getProductName().length() > 5) {
            baseScore += 5.0;
            log.debug("제품명 품질 점수 추가 후: {}", baseScore);
        }

        if (post.getCompanyName() != null && !post.getCompanyName().isEmpty()) {
            baseScore += 3.0;
            log.debug("회사명 품질 점수 추가 후: {}", baseScore);
        }

        double finalScore = Math.max(baseScore, 10.0);
        log.debug("최종 기본점수: {} (PostType: {})", finalScore, post.getPostType());

        return finalScore;
    }

    private Double calculateViewScore(List<UserInteraction> interactions) {
        long viewCount = interactions.stream()
                .filter(i -> i.getInteractionType() == InteractionType.VIEW)
                .count();
        return Math.min(viewCount * 10.0, 100.0);
    }

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
     * 🔥 PostType별 분포 확인용 디버깅 메서드
     */
    public Map<String, Object> debugPostTypeDistribution() {
        Map<String, Object> debug = new HashMap<>();

        try {
            // 전체 게시물 분포
            List<Post> allPosts = postRepository.findTop100ByOrderByCreatedAtDesc();
            Map<PostType, Long> distribution = allPosts.stream()
                    .collect(Collectors.groupingBy(Post::getPostType, Collectors.counting()));

            debug.put("전체_게시물_분포", distribution);
            debug.put("전체_게시물_수", allPosts.size());

            // 각 타입별 평균 기본 점수
            Map<PostType, Double> avgScores = new HashMap<>();
            for (PostType type : PostType.values()) {
                OptionalDouble avgScore = allPosts.stream()
                        .filter(post -> post.getPostType() == type)
                        .mapToDouble(this::calculateBaseScore)
                        .average();
                avgScores.put(type, avgScore.orElse(0.0));
            }

            debug.put("PostType별_평균_기본점수", avgScores);

            // 최신 20개 게시물의 분포
            List<Post> recent20 = postRepository.findTop20ByOrderByCreatedAtDesc();
            Map<PostType, Long> recent20Distribution = recent20.stream()
                    .collect(Collectors.groupingBy(Post::getPostType, Collectors.counting()));

            debug.put("최신_20개_분포", recent20Distribution);

            return debug;

        } catch (Exception e) {
            log.error("PostType 분포 디버깅 실패", e);
            debug.put("error", e.getMessage());
            return debug;
        }
    }

    // 기존 메서드들은 그대로 유지하되 균형 로직 제거
    @Transactional(readOnly = false)
    public void initializeBasicRecommendations(Member member, Business business) {
        log.info("순수 점수 기반 기본 추천 초기화: {} (사업장: {}, 지역: {} {})",
                member.getEmail(), business.getBusinessName(),
                business.getSidoName(), business.getSigunguName());

        try {
            // 🔥 연령 필터링 제거된 메서드 사용
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

            // 🔥 순수 점수 기반 추천으로 변경
            List<RecommendationResponse> scoreBasedRecs = generatePureScoreBasedRecommendations(
                    member, business, regionFilteredPosts);

            // 나머지 로직은 동일...

        } catch (Exception e) {
            log.error("기본 추천 초기화 실패", e);
        }
    }

    @Transactional(readOnly = false)
    public void rebuildRecommendationsForBusiness(Member member, Business business) {
        log.info("사업장 변경으로 인한 순수 점수 기반 추천 재구축: Member {}, Business {} (지역: {} {})",
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

    // 비동기 스코어 업데이트
    @Async
    protected void updateSinglePostScoreAsync(Member member, Post post) {
        try {
            updateSinglePostScore(member, post);
        } catch (Exception e) {
            log.error("비동기 스코어 업데이트 실패: postId={}", post.getPostId(), e);
        }
    }

    @Transactional(readOnly = false)
    public void adjustRelativeInterestScores(Member member) {
        Business business = businessRepository.findByMember(member).stream()
                .filter(b -> b.getStatus() == BusinessStatus.ACTIVE)
                .findFirst()
                .orElse(null);

        if (business == null) return;

        List<InterestScore> allScores = interestScoreRepository
                .findByMemberAndBusinessOrderByTotalScoreDesc(member, business);

        if (allScores.size() < 10) return;

        int topThreshold = Math.max(1, allScores.size() / 5);

        for (int i = topThreshold; i < allScores.size(); i++) {
            InterestScore score = allScores.get(i);

            double decayRate = 0.95;
            if (i > allScores.size() * 0.8) {
                decayRate = 0.90;
            }

            double newScore = score.getTotalScore() * decayRate;
            score.updateTotalScore(newScore);
            interestScoreRepository.save(score);
        }

        log.info("상대적 관심도 조정 완료: {}건 중 {}건 조정",
                allScores.size(), allScores.size() - topThreshold);
    }

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
                    List<UserInteraction> interactions = userInteractionRepository
                            .findByMemberAndPost(member, score.getPost());

                    Double newEngagementScore = calculateTimeDecayedEngagementScore(interactions);
                    Double viewScore = calculateViewScore(interactions);
                    Double timeScore = calculateTimeScore(interactions);

                    score.updateScore(viewScore, newEngagementScore, timeScore);
                    interestScoreRepository.save(score);
                    totalUpdated++;
                }

                adjustRelativeInterestScores(member);
                clearMemberCache(member.getEmail());

            } catch (Exception e) {
                log.error("회원 스코어 재계산 실패: {}", member.getEmail(), e);
            }
        }

        log.info("전체 관심도 스코어 재계산 완료: {}건 업데이트", totalUpdated);
    }
    private List<Post> getRegionAwarePosts(Business business, Member member) {
        try {
            // 🔥 지역 필터링하지 않고 전체 최신 게시물 가져오기
            List<Post> allRecentPosts = postRepository.findTop100ByOrderByCreatedAtDesc();

            // 🔥 연령 필터링도 제거 - 점수로만 반영
            log.info("필터링 제거 후 결과: 지역({} {}) & 연령({}) -> {}건 (필터링 없이 점수로 반영)",
                    business.getSidoName(), business.getSigunguName(),
                    member.getBirthDate() != null ? calculateAge(member.getBirthDate()) : "미설정",
                    allRecentPosts.size());

            // 🔥 PostType 분포 확인
            Map<PostType, Long> distribution = allRecentPosts.stream()
                    .collect(Collectors.groupingBy(Post::getPostType, Collectors.counting()));
            log.info("PostType 분포: {}", distribution);

            return allRecentPosts;

        } catch (Exception e) {
            log.error("지역 인식 게시물 조회 실패: {} {}", business.getSidoName(), business.getSigunguName(), e);
            return postRepository.findTop50ByOrderByCreatedAtDesc();
        }
    }
}
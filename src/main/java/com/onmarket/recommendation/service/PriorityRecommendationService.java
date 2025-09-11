package com.onmarket.recommendation.service;

import com.onmarket.business.domain.Business;
import com.onmarket.member.domain.Member;
import com.onmarket.post.domain.Post;
import com.onmarket.recommendation.domain.InteractionType;
import com.onmarket.recommendation.domain.InterestScore;
import com.onmarket.recommendation.domain.UserInteraction;
import com.onmarket.recommendation.dto.RecommendationResponse;
import com.onmarket.recommendation.repository.InterestScoreRepository;
import com.onmarket.recommendation.repository.UserInteractionRepository;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PriorityRecommendationService {

    private final RegionFilterService regionFilterService;
    private final AgeFilterService ageFilterService;
    private final InterestScoreRepository interestScoreRepository;
    private final UserInteractionRepository userInteractionRepository;

    // 가중치 상수
    private static final double INTEREST_WEIGHT = 0.4;    // 관심도 40%
    private static final double REGION_WEIGHT = 0.3;      // 지역 30%
    private static final double AGE_WEIGHT = 0.2;         // 연령 20%
    private static final double RECENCY_WEIGHT = 0.1;     // 최신성 10%

    private static final double MINIMUM_TOTAL_SCORE = 5.0;  // 10.0 -> 5.0으로 낮춤
    private static final double MINIMUM_INTEREST_SCORE = 3.0; // 5.0 -> 3.0으로 낮춤
    private static final double NEGATIVE_FEEDBACK_MULTIPLIER = 2.5;

    /**
     * 🔥 0점 문제 해결된 우선순위 기반 추천 생성
     */
    public List<RecommendationResponse> generatePriorityRecommendations(
            Member member, Business business, List<Post> posts, int limit) {

        log.info("우선순위 기반 추천 생성 시작: {} 개 게시물, 요청 개수: {}", posts.size(), limit);

        if (posts.isEmpty()) {
            log.warn("분석할 게시물이 없음");
            return Collections.emptyList();
        }

        // 1. 각 게시물에 대해 우선순위 스코어 계산
        List<PostPriorityScore> priorityScores = posts.stream()
                .map(post -> calculatePriorityScore(member, business, post))
                .filter(Objects::nonNull)
                .filter(this::isValidScore) // 🔥 유효한 점수만 필터링
                .collect(Collectors.toList());

        log.info("유효한 점수({} 이상) 게시물: {} / {} 건",
                MINIMUM_TOTAL_SCORE, priorityScores.size(), posts.size());

        // 점수 분포 로깅
        logScoreDistribution(priorityScores);

        // 2. 정렬 (높은 순, 동점자 처리)
        priorityScores.sort(this::comparePostPriorityScores);

        // 3. 상위 N개 선택하여 RecommendationResponse로 변환
        List<RecommendationResponse> recommendations = priorityScores.stream()
                .limit(limit)
                .map(this::convertToRecommendationResponse)
                .collect(Collectors.toList());

        log.info("최종 추천 완료: {} 개 반환", recommendations.size());

        return recommendations;
    }

    /**
     * 🔥 유효한 점수인지 확인 (0점 문제 해결)
     */
    private boolean isValidScore(PostPriorityScore score) {
        boolean isValid = score.getTotalScore() >= MINIMUM_TOTAL_SCORE &&
                score.getInterestScore() >= MINIMUM_INTEREST_SCORE;

        if (!isValid) {
            log.debug("점수 부족으로 제외: PostId={}, PostType={}, 총점={}, 관심도={}",
                    score.getPost().getPostId(),
                    score.getPost().getPostType(), // 🔥 PostType 추가
                    String.format("%.1f", score.getTotalScore()),
                    String.format("%.1f", score.getInterestScore()));
        }

        return isValid;
    }

    /**
     * 🔥 점수 분포 로깅 (디버깅용)
     */
    private void logScoreDistribution(List<PostPriorityScore> scores) {
        if (scores.isEmpty()) return;

        DoubleSummaryStatistics interestStats = scores.stream()
                .mapToDouble(PostPriorityScore::getInterestScore)
                .summaryStatistics();

        DoubleSummaryStatistics totalStats = scores.stream()
                .mapToDouble(PostPriorityScore::getTotalScore)
                .summaryStatistics();

        log.info("점수 분포 - 관심도: 평균={}, 최대={}, 최소={} | 총점: 평균={}, 최대={}, 최소={}",
                String.format("%.1f", interestStats.getAverage()),
                String.format("%.1f", interestStats.getMax()),
                String.format("%.1f", interestStats.getMin()),
                String.format("%.1f", totalStats.getAverage()),
                String.format("%.1f", totalStats.getMax()),
                String.format("%.1f", totalStats.getMin()));
    }

    /**
     * 동점자 처리 비교 로직
     */
    private int comparePostPriorityScores(PostPriorityScore a, PostPriorityScore b) {
        int totalScoreCompare = Double.compare(b.getTotalScore(), a.getTotalScore());
        if (totalScoreCompare != 0) return totalScoreCompare;

        int interestCompare = Double.compare(b.getInterestScore(), a.getInterestScore());
        if (interestCompare != 0) return interestCompare;

        int regionCompare = Double.compare(b.getRegionScore(), a.getRegionScore());
        if (regionCompare != 0) return regionCompare;

        int recencyCompare = Double.compare(b.getRecencyScore(), a.getRecencyScore());
        if (recencyCompare != 0) return recencyCompare;

        if (a.getPost().getCreatedAt() != null && b.getPost().getCreatedAt() != null) {
            return b.getPost().getCreatedAt().compareTo(a.getPost().getCreatedAt());
        }

        return b.getPost().getPostId().compareTo(a.getPost().getPostId());
    }

    /**
     * 🔥 개선된 관심도 스코어 조회 (0점 방지)
     */
    private double getInterestScoreWithDuplicateHandling(Member member, Business business, Post post) {
        try {
            List<InterestScore> scores = interestScoreRepository
                    .findByMemberAndBusinessAndPostOrderByLastCalculatedAtDesc(member, business, post);

            double baseScore;

            if (scores.isEmpty()) {
                // 관심도 스코어가 없으면 실시간 계산
                baseScore = calculateBaseInterestFromInteractions(member, post);
                log.debug("DB 스코어 없음, 실시간 계산: {} (PostId: {})", baseScore, post.getPostId());
            } else {
                // 첫 번째 (가장 최신) 스코어 사용
                InterestScore latestScore = scores.get(0);

                // 중복 처리
                if (scores.size() > 1) {
                    log.warn("중복 InterestScore 발견 및 정리: {}개", scores.size());
                    try {
                        List<InterestScore> duplicates = scores.subList(1, scores.size());
                        interestScoreRepository.deleteAll(duplicates);
                    } catch (Exception e) {
                        log.error("중복 데이터 삭제 실패", e);
                    }
                }

                baseScore = latestScore.getTotalScore();
                log.debug("DB에서 스코어 조회: {} (PostId: {})", baseScore, post.getPostId());
            }

            // 🔥 부정적 피드백 추가 반영
            double negativePenalty = calculateNegativeFeedbackPenalty(member, post);
            double finalScore = Math.max(0.0, baseScore - negativePenalty);

            // 🔥 최소 점수 보장 (완전히 0이 되는 것 방지)
            if (finalScore == 0.0 && baseScore > 0.0) {
                finalScore = Math.min(baseScore * 0.1, 3.0); // 원래 점수의 10% 또는 최대 3점
                log.debug("0점 방지 조정: {} -> {} (PostId: {})", 0.0, finalScore, post.getPostId());
            }

            log.debug("최종 관심도 스코어: 기본={}, 감점={}, 최종={} (PostId: {})",
                    baseScore, negativePenalty, finalScore, post.getPostId());

            return finalScore;

        } catch (Exception e) {
            log.error("관심도 스코어 조회 실패: postId={}", post.getPostId(), e);
            return 0.0;
        }
    }


    /**
     * 🔥 강화된 부정적 피드백 패널티 계산
     */
    private double calculateNegativeFeedbackPenalty(Member member, Post post) {
        try {
            LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
            List<UserInteraction> recentInteractions = userInteractionRepository
                    .findByMemberAndPostAndCreatedAtAfter(member, post, oneMonthAgo);

            double totalPenalty = 0.0;
            LocalDateTime now = LocalDateTime.now();

            for (UserInteraction interaction : recentInteractions) {
                double penaltyScore = 0.0;

                switch (interaction.getInteractionType()) {
                    case UNSCRAP -> {
                        // 🔥 스크랩 해제 강화된 감점
                        penaltyScore = 80.0; // 기존 25.0 -> 80.0
                        log.debug("스크랩 해제 감점: {}점 (PostId: {})", penaltyScore, post.getPostId());
                    }
                    case RATING -> {
                        if (interaction.getRating() != null && interaction.getRating() <= 2) {
                            // 🔥 낮은 평점 강화된 감점
                            penaltyScore = interaction.getRating() == 1 ? 100.0 : 60.0; // 1점: 100점, 2점: 60점 감점
                            log.debug("낮은 평점({}) 감점: {}점 (PostId: {})",
                                    interaction.getRating(), penaltyScore, post.getPostId());
                        }
                    }
                    // 🔥 추가 부정적 신호들
                    case VIEW -> {
                        // 매우 짧은 조회시간 (5초 미만)은 부정적 신호
                        if (interaction.getDurationSeconds() != null && interaction.getDurationSeconds() < 5) {
                            penaltyScore = 15.0;
                            log.debug("짧은 조회시간 감점: {}점 ({}초, PostId: {})",
                                    penaltyScore, interaction.getDurationSeconds(), post.getPostId());
                        }
                    }
                    case SCROLL -> {
                        // 매우 낮은 스크롤 비율 (10% 미만)은 부정적 신호
                        if (interaction.getScrollPercentage() != null && interaction.getScrollPercentage() < 10) {
                            penaltyScore = 10.0;
                            log.debug("낮은 스크롤 감점: {}점 ({}%, PostId: {})",
                                    penaltyScore, interaction.getScrollPercentage(), post.getPostId());
                        }
                    }
                }

                // 🔥 시간에 따른 감점 가중치 (최근일수록 더 강하게)
                if (penaltyScore > 0) {
                    long hoursAgo = ChronoUnit.HOURS.between(interaction.getCreatedAt(), now);
                    double timeWeight = calculateNegativeTimeWeight(hoursAgo);
                    double weightedPenalty = penaltyScore * timeWeight * NEGATIVE_FEEDBACK_MULTIPLIER;

                    totalPenalty += weightedPenalty;
                    log.debug("부정적 피드백: 기본={}점, 시간가중치={}, 최종={}점",
                            penaltyScore, timeWeight, weightedPenalty);
                }
            }

            return Math.min(totalPenalty, 150.0); // 최대 150점까지 감점

        } catch (Exception e) {
            log.error("부정적 피드백 계산 실패: postId={}", post.getPostId(), e);
            return 0.0;
        }
    }

    /**
     * 🔥 부정적 피드백용 시간 가중치 (최근일수록 더 강하게 반영)
     */
    private double calculateNegativeTimeWeight(long hoursAgo) {
        if (hoursAgo < 1) {
            return 2.0; // 1시간 이내: 200% (매우 강하게)
        } else if (hoursAgo < 24) {
            return 1.8; // 1일 이내: 180%
        } else if (hoursAgo < 168) { // 7일
            return 1.5; // 1주일 이내: 150%
        } else if (hoursAgo < 720) { // 30일
            return 1.2; // 1개월 이내: 120%
        } else {
            return 1.0; // 1개월 이후: 100%
        }
    }

    /**
     * 🔥 기본 관심도 계산 개선 (상호작용이 없어도 최소 점수)
     */
    private double calculateBaseInterestFromInteractions(Member member, Post post) {
        try {
            List<UserInteraction> interactions = userInteractionRepository
                    .findByMemberAndPost(member, post);

            if (interactions.isEmpty()) {
                // 🔥 상호작용이 없어도 기본 점수 부여 (게시물 속성 기반)
                double baseScore = calculatePostBaseScore(post);
                log.debug("상호작용 없음, 게시물 기본 점수: {} (PostId: {})", baseScore, post.getPostId());
                return baseScore;
            }

            double totalScore = 0.0;
            LocalDateTime now = LocalDateTime.now();

            for (UserInteraction interaction : interactions) {
                double baseScore = getEnhancedInteractionScore(interaction);
                long hoursAgo = ChronoUnit.HOURS.between(interaction.getCreatedAt(), now);
                double timeWeight = calculateTimeWeight(hoursAgo);

                if (baseScore < 0) {
                    timeWeight = calculateNegativeTimeWeight(hoursAgo);
                }

                totalScore += baseScore * timeWeight;
            }

            // 🔥 점수 범위 조정 (최소값 보장)
            double finalScore = Math.max(5.0, Math.min(totalScore, 120.0)); // 최소 5점

            log.debug("상호작용 기반 관심도: {}개 상호작용 -> {} 점 (PostId: {})",
                    interactions.size(), finalScore, post.getPostId());

            return finalScore;

        } catch (Exception e) {
            log.error("상호작용 기반 관심도 계산 실패: postId={}", post.getPostId(), e);
            return calculatePostBaseScore(post); // 오류 시 기본 점수
        }
    }

    /**
     * 🔥 게시물 기본 점수 계산 (상호작용이 없을 때)
     */
    private double calculatePostBaseScore(Post post) {
        double baseScore = 15.0; // 기본 점수

        // 🔥 PostType별 점수 완전 동일화
        baseScore += 10.0; // 모든 PostType에 동일한 점수

        // 최신성 가산점
        if (post.getCreatedAt() != null) {
            long daysSinceCreated = java.time.Duration.between(
                    post.getCreatedAt(), LocalDateTime.now()).toDays();

            if (daysSinceCreated <= 7) {
                baseScore += 10.0; // 1주일 이내 신규
            } else if (daysSinceCreated <= 30) {
                baseScore += 5.0;  // 1개월 이내
            }
        }

        // 품질 점수
        if (post.getProductName() != null && post.getProductName().length() > 10) {
            baseScore += 3.0;
        }

        if (post.getCompanyName() != null && !post.getCompanyName().isEmpty()) {
            baseScore += 2.0;
        }

        return Math.min(baseScore, 50.0);
    }


    /**
     * 🔥 강화된 상호작용 점수 계산
     */
    private double getEnhancedInteractionScore(UserInteraction interaction) {
        switch (interaction.getInteractionType()) {
            case VIEW -> {
                // 조회 시간에 따른 차등 점수
                if (interaction.getDurationSeconds() != null) {
                    if (interaction.getDurationSeconds() < 5) {
                        return 2.0; // 매우 짧은 조회
                    } else if (interaction.getDurationSeconds() < 30) {
                        return 8.0; // 일반 조회
                    } else {
                        return 15.0; // 긴 조회
                    }
                }
                return 8.0; // 기본 조회
            }
            case SCRAP -> { return 60.0; } // 50.0 -> 60.0
            case UNSCRAP -> { return -80.0; } // 🔥 -25.0 -> -80.0 (강화된 감점)
            case COMMENT -> { return 45.0; } // 40.0 -> 45.0
            case CLICK_LINK -> { return 70.0; } // 60.0 -> 70.0
            case SCROLL -> {
                if (interaction.getScrollPercentage() != null) {
                    if (interaction.getScrollPercentage() < 10) {
                        return -5.0; // 🔥 매우 낮은 스크롤은 부정적
                    } else if (interaction.getScrollPercentage() < 30) {
                        return 3.0;
                    } else if (interaction.getScrollPercentage() < 70) {
                        return 8.0;
                    } else {
                        return 15.0; // 높은 스크롤
                    }
                }
                return 5.0;
            }
            case RATING -> {
                if (interaction.getRating() != null) {
                    if (interaction.getRating() == 1) {
                        return -100.0; // 🔥 1점: -100점 (매우 강한 감점)
                    } else if (interaction.getRating() == 2) {
                        return -60.0; // 🔥 2점: -60점 (강한 감점)
                    } else if (interaction.getRating() == 3) {
                        return 5.0; // 3점: 약간의 점수
                    } else if (interaction.getRating() == 4) {
                        return 40.0; // 4점: 좋은 점수
                    } else if (interaction.getRating() == 5) {
                        return 80.0; // 5점: 매우 좋은 점수
                    }
                }
                return 0.0;
            }
            default -> { return 0.0; }
        }
    }



    /**
     * 시간 가중치 계산 (기존과 동일)
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

    private PostPriorityScore calculatePriorityScore(Member member, Business business, Post post) {
        try {
            // 1. 강화된 관심도 스코어 (부정적 피드백 포함)
            double interestScore = getInterestScoreWithDuplicateHandling(member, business, post);

            // 2. 지역 우선순위 스코어
            double regionScore = calculateRegionPriorityScore(business, post);

            // 3. 연령 우선순위 스코어
            double ageScore = ageFilterService.calculateAgePriorityScore(member, post);

            // 4. 최신성 스코어
            double recencyScore = calculateRecencyScore(post);

            // 5. 가중치 적용한 종합 스코어 계산
            double totalScore = calculateWeightedScore(interestScore, regionScore, ageScore, recencyScore);

            return PostPriorityScore.builder()
                    .post(post)
                    .interestScore(interestScore)
                    .regionScore(regionScore)
                    .ageScore(ageScore)
                    .recencyScore(recencyScore)
                    .totalScore(totalScore)
                    .build();

        } catch (Exception e) {
            log.error("우선순위 스코어 계산 실패: postId={}", post.getPostId(), e);
            return null;
        }
    }

    /**
     * 지역 우선순위 스코어 계산
     */
    public double calculateRegionPriorityScore(Business business, Post post) {
        if (business == null || business.getSidoName() == null) {
            return 50.0; // 기본 점수
        }

        try {
            // RegionFilterService를 사용하여 지역 매칭 확인
            List<Post> regionFilteredPosts = regionFilterService.filterPostsByRegion(
                    business.getSidoName(), business.getSigunguName());

            boolean isRegionMatch = regionFilteredPosts.stream()
                    .anyMatch(filteredPost -> filteredPost.getPostId().equals(post.getPostId()));

            if (isRegionMatch) {
                // 시군구까지 매칭되면 더 높은 점수
                if (business.getSigunguName() != null) {
                    return 100.0; // 완전 지역 매칭
                } else {
                    return 80.0; // 시도만 매칭
                }
            } else {
                return 30.0; // 지역 매칭 안됨
            }
        } catch (Exception e) {
            log.warn("지역 스코어 계산 실패: postId={}", post.getPostId(), e);
            return 50.0; // 오류시 기본 점수
        }
    }

    /**
     * 가중치 적용한 종합 스코어 계산
     */
    private double calculateWeightedScore(double interestScore, double regionScore,
                                          double ageScore, double recencyScore) {

        // 각 스코어를 0-100 범위로 정규화
        double normalizedInterest = Math.min(Math.max(interestScore, 0), 100);
        double normalizedRegion = Math.min(Math.max(regionScore, 0), 100);
        double normalizedAge = Math.min(Math.max(ageScore, 0), 100);
        double normalizedRecency = Math.min(Math.max(recencyScore, 0), 100);

        return (normalizedInterest * INTEREST_WEIGHT) +
                (normalizedRegion * REGION_WEIGHT) +
                (normalizedAge * AGE_WEIGHT) +
                (normalizedRecency * RECENCY_WEIGHT);
    }

    /**
     * 최신성 스코어 계산
     */
    private double calculateRecencyScore(Post post) {
        if (post.getCreatedAt() == null) {
            return 50.0; // 생성일이 없으면 중간 점수
        }

        long daysSinceCreated = java.time.Duration.between(
                post.getCreatedAt(), LocalDateTime.now()).toDays();

        // 최신일수록 높은 점수 (최대 100점)
        // 0일 = 100점, 30일 = 70점, 60일 = 40점, 90일 = 10점
        double score = Math.max(10, 100.0 - (daysSinceCreated * 1.0));

        return Math.min(score, 100.0);
    }

    /**
     * 🔥 RecommendationResponse 변환 시 점수 보정
     */
    private RecommendationResponse convertToRecommendationResponse(PostPriorityScore priorityScore) {
        Post post = priorityScore.getPost();

        // 🔥 최종 점수 검증 및 보정
        double finalInterestScore = Math.max(priorityScore.getInterestScore(), MINIMUM_INTEREST_SCORE);

        return RecommendationResponse.builder()
                .postId(post.getPostId())
                .productName(post.getProductName())
                .companyName(post.getCompanyName())
                .postType(post.getPostType())
                .deadline(post.getDeadline())
                .summary(post.getSummary())
                .imageUrl(post.getImageUrl())
                .interestScore(finalInterestScore) // 🔥 보정된 점수 사용
                .recommendationReason(generateRecommendationReason(priorityScore))
                .build();
    }

    /**
     * 🔥 개선된 추천 이유 생성 - 더 정확하고 즉시 반영
     */
    private String generateRecommendationReason(PostPriorityScore score) {
        List<String> reasons = new ArrayList<>();
        Post post = score.getPost();

        // 🔥 실시간 행동 기반 이유 (최우선)
        String behaviorReason = generateBehaviorBasedReason(post);
        if (behaviorReason != null) {
            reasons.add(behaviorReason);
        }

        // 관심도 기반 이유 (임계값 조정)
        if (score.getInterestScore() > 80) {
            reasons.add("매우 높은 관심도");
        } else if (score.getInterestScore() > 50) {
            reasons.add("높은 관심도");
        } else if (score.getInterestScore() > 20) {
            reasons.add("관심 표시");
        } else if (score.getInterestScore() > 5) {
            reasons.add("조회 이력");
        } else if (score.getInterestScore() > 0) {
            reasons.add("추천");
        }

        // 지역 스코어
        if (score.getRegionScore() > 95) {
            reasons.add("완벽한 지역 매칭");
        } else if (score.getRegionScore() > 80) {
            reasons.add("지역 매칭");
        } else if (score.getRegionScore() > 60) {
            reasons.add("지역 관련");
        }

        // 연령 스코어
        if (score.getAgeScore() > 85) {
            reasons.add("연령 최적화");
        } else if (score.getAgeScore() > 70) {
            reasons.add("연령 적합");
        } else if (score.getAgeScore() > 50) {
            reasons.add("연령대 관련");
        }

        // 최신성 스코어
        if (score.getRecencyScore() > 98) {
            reasons.add("최신 등록");
        } else if (score.getRecencyScore() > 90) {
            reasons.add("신규 상품");
        } else if (score.getRecencyScore() > 70) {
            reasons.add("최근 상품");
        }

        // 이유가 없으면 기본값
        if (reasons.isEmpty()) {
            // 가장 높은 스코어를 기준으로 기본 이유 설정
            double maxScore = Math.max(Math.max(score.getRegionScore(), score.getAgeScore()),
                    Math.max(score.getInterestScore(), score.getRecencyScore()));

            if (maxScore == score.getInterestScore() && maxScore > 0) {
                reasons.add("맞춤 추천");
            } else if (maxScore == score.getRegionScore()) {
                reasons.add("지역 추천");
            } else if (maxScore == score.getRecencyScore()) {
                reasons.add("신규 상품");
            } else if (maxScore == score.getAgeScore()) {
                reasons.add("연령 맞춤");
            } else {
                reasons.add("추천 상품");
            }
        }

        String finalReason = String.join(" · ", reasons);

        log.debug("실시간 추천 이유 생성: PostId={}, 이유={}, 행동기반={}, 스코어=[관심:{}, 지역:{}, 연령:{}, 최신:{}]",
                post.getPostId(), finalReason, behaviorReason,
                String.format("%.1f", score.getInterestScore()),
                String.format("%.1f", score.getRegionScore()),
                String.format("%.1f", score.getAgeScore()),
                String.format("%.1f", score.getRecencyScore()));

        return finalReason;
    }
    /**
     * 🔥 개선된 행동 기반 추천 이유 생성 - 즉시 반영
     */
    private String generateBehaviorBasedReason(Post post) {
        try {
            // 🔥 최근 상호작용을 실시간으로 조회 (캐시 없이)
            List<UserInteraction> recentInteractions = getRecentInteractionsForPost(post);

            log.debug("행동 기반 이유 분석: PostId={}, 최근상호작용={}건",
                    post.getPostId(), recentInteractions.size());

            if (recentInteractions.isEmpty()) {
                return null;
            }

            // 상호작용 유형별 분석
            Map<InteractionType, Long> interactionCounts = recentInteractions.stream()
                    .collect(Collectors.groupingBy(
                            UserInteraction::getInteractionType,
                            Collectors.counting()));

            log.debug("상호작용 분포: {}", interactionCounts);

            // 🔥 우선순위별로 더 자세한 이유 생성
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

            if (interactionCounts.getOrDefault(InteractionType.RATING, 0L) > 0) {
                // 평점 정보도 고려
                OptionalDouble avgRating = recentInteractions.stream()
                        .filter(i -> i.getInteractionType() == InteractionType.RATING && i.getRating() != null)
                        .mapToInt(UserInteraction::getRating)
                        .average();

                if (avgRating.isPresent() && avgRating.getAsDouble() >= 4.0) {
                    return "높은 평점을 준 유사 상품";
                } else {
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

            // 스크롤 상호작용도 고려
            if (interactionCounts.getOrDefault(InteractionType.SCROLL, 0L) > 0) {
                long scrollCount = interactionCounts.get(InteractionType.SCROLL);
                if (scrollCount >= 5) {
                    return "자세히 본 상품";
                } else {
                    return "관심 있게 본 상품";
                }
            }

            return null;

        } catch (Exception e) {
            log.warn("행동 기반 이유 생성 실패: postId={}", post.getPostId(), e);
            return null;
        }
    }
    /**
     * 🔥 개선된 최근 상호작용 조회 - Repository 메서드 정확히 사용
     */
    private List<UserInteraction> getRecentInteractionsForPost(Post post) {
        try {
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

            // 🔥 Repository에 정의된 메서드 사용
            return userInteractionRepository.findByPostAndCreatedAtAfter(post, sevenDaysAgo);

        } catch (Exception e) {
            log.warn("최근 상호작용 조회 실패: postId={}", post.getPostId(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 🔥 추천 이유 디버깅용 메서드
     */
    public Map<String, Object> debugRecommendationReason(Member member, Business business, Post post) {
        Map<String, Object> debugInfo = new HashMap<>();

        try {
            PostPriorityScore score = calculatePriorityScore(member, business, post);

            if (score == null) {
                debugInfo.put("error", "스코어 계산 실패");
                return debugInfo;
            }

            debugInfo.put("우선순위_스코어", Map.of(
                    "관심도", score.getInterestScore(),
                    "지역", score.getRegionScore(),
                    "연령", score.getAgeScore(),
                    "최신성", score.getRecencyScore(),
                    "총합", score.getTotalScore()
            ));

            debugInfo.put("생성된_추천이유", generateRecommendationReason(score));

            // 행동 기반 이유
            String behaviorReason = generateBehaviorBasedReason(post);
            debugInfo.put("행동_기반_이유", behaviorReason);

            // 최근 상호작용
            List<UserInteraction> recentInteractions = getRecentInteractionsForPost(post);
            debugInfo.put("최근_상호작용_수", recentInteractions.size());
            debugInfo.put("최근_상호작용_유형", recentInteractions.stream()
                    .collect(Collectors.groupingBy(
                            UserInteraction::getInteractionType,
                            Collectors.counting())));

            return debugInfo;

        } catch (Exception e) {
            log.error("추천 이유 디버깅 실패", e);
            debugInfo.put("error", e.getMessage());
            return debugInfo;
        }
    }

    /**
     * 🔥 사용자별 추천 품질 분석
     */
    public Map<String, Object> analyzeRecommendationQuality(Member member, Business business,
                                                            List<Post> posts) {
        Map<String, Object> analysis = new HashMap<>();

        try {
            List<PostPriorityScore> scores = posts.stream()
                    .map(post -> calculatePriorityScore(member, business, post))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (scores.isEmpty()) {
                analysis.put("message", "분석할 게시물이 없음");
                return analysis;
            }

            // 스코어 분포 분석
            DoubleSummaryStatistics interestStats = scores.stream()
                    .mapToDouble(PostPriorityScore::getInterestScore)
                    .summaryStatistics();

            DoubleSummaryStatistics totalStats = scores.stream()
                    .mapToDouble(PostPriorityScore::getTotalScore)
                    .summaryStatistics();

            analysis.put("게시물_수", scores.size());
            analysis.put("관심도_스코어_분포", Map.of(
                    "평균", String.format("%.2f", interestStats.getAverage()),
                    "최대", interestStats.getMax(),
                    "최소", interestStats.getMin()
            ));
            analysis.put("총합_스코어_분포", Map.of(
                    "평균", String.format("%.2f", totalStats.getAverage()),
                    "최대", totalStats.getMax(),
                    "최소", totalStats.getMin()
            ));

            // 높은 관심도를 가진 게시물 비율
            long highInterestCount = scores.stream()
                    .filter(s -> s.getInterestScore() > 20)
                    .count();

            analysis.put("높은_관심도_비율", String.format("%.1f%%",
                    (highInterestCount * 100.0) / scores.size()));

            // 상위 3개 추천 이유 분석
            List<String> topReasons = scores.stream()
                    .sorted((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()))
                    .limit(3)
                    .map(this::generateRecommendationReason)
                    .collect(Collectors.toList());

            analysis.put("상위_3개_추천이유", topReasons);

            return analysis;

        } catch (Exception e) {
            log.error("추천 품질 분석 실패", e);
            analysis.put("error", e.getMessage());
            return analysis;
        }
    }

    /**
     * 게시물 우선순위 스코어 데이터 클래스
     */
    @lombok.Builder
    @lombok.Getter
    @lombok.ToString
    public static class PostPriorityScore {
        private final Post post;
        private final double interestScore;
        private final double regionScore;
        private final double ageScore;
        private final double recencyScore;
        private final double totalScore;
    }
}
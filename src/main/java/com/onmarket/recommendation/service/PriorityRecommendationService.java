package com.onmarket.recommendation.service;

import com.onmarket.business.domain.Business;
import com.onmarket.member.domain.Member;
import com.onmarket.post.domain.Post;
import com.onmarket.recommendation.domain.InterestScore;
import com.onmarket.recommendation.dto.RecommendationResponse;
import com.onmarket.recommendation.repository.InterestScoreRepository;
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

    // 가중치 상수
    private static final double INTEREST_WEIGHT = 0.4;    // 관심도 40%
    private static final double REGION_WEIGHT = 0.3;      // 지역 30%
    private static final double AGE_WEIGHT = 0.2;         // 연령 20%
    private static final double RECENCY_WEIGHT = 0.1;     // 최신성 10%

    /**
     * 우선순위 기반 추천 생성
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
                .collect(Collectors.toList());

        // 2. 종합 스코어로 정렬 (높은 순)
        priorityScores.sort((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()));

        // 3. 상위 N개 선택하여 RecommendationResponse로 변환
        List<RecommendationResponse> recommendations = priorityScores.stream()
                .limit(limit)
                .map(this::convertToRecommendationResponse)
                .collect(Collectors.toList());

        log.info("우선순위 추천 완료: {} 개 반환", recommendations.size());

        // 디버깅용 로그 (상위 5개만)
        priorityScores.stream()
                .limit(5)
                .forEach(score -> log.debug("Post {}: 관심도={}, 지역={}, 연령={}, 최신성={}, 총합={}",
                        score.getPost().getPostId(),
                        String.format("%.1f", score.getInterestScore()),
                        String.format("%.1f", score.getRegionScore()),
                        String.format("%.1f", score.getAgeScore()),
                        String.format("%.1f", score.getRecencyScore()),
                        String.format("%.1f", score.getTotalScore())));

        return recommendations;
    }

    /**
     * 게시물별 우선순위 스코어 계산
     */
    private PostPriorityScore calculatePriorityScore(Member member, Business business, Post post) {
        try {
            // 🔥 중복 처리 로직 추가
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
            return null; // 필터링됨
        }
    }

    /**
     * 🔥 중복 처리가 포함된 관심도 스코어 조회
     */
    private double getInterestScoreWithDuplicateHandling(Member member, Business business, Post post) {
        try {
            // Repository에 추가할 메서드 (또는 기존 메서드가 있다면 사용)
            List<InterestScore> scores = interestScoreRepository
                    .findByMemberAndBusinessAndPostOrderByLastCalculatedAtDesc(member, business, post);

            if (scores.isEmpty()) {
                return 0.0; // 기본값
            }

            // 첫 번째 (가장 최신) 스코어 사용
            InterestScore latestScore = scores.get(0);

            // 중복이 있으면 정리
            if (scores.size() > 1) {
                log.warn("중복 InterestScore 발견: Member {}, Post {}, Business {} - {}개 중복",
                        member.getEmail(), post.getPostId(), business.getBusinessId(), scores.size());

                // 나머지 삭제 (비동기로 처리하거나 별도 배치로 처리하는 것이 좋음)
                List<InterestScore> duplicates = scores.subList(1, scores.size());
                try {
                    interestScoreRepository.deleteAll(duplicates);
                    log.info("중복 InterestScore 삭제 완료: {}개", duplicates.size());
                } catch (Exception deleteError) {
                    log.error("중복 데이터 삭제 실패", deleteError);
                }
            }

            return latestScore.getTotalScore();

        } catch (Exception e) {
            log.error("관심도 스코어 조회 실패: postId={}", post.getPostId(), e);
            return 0.0; // 예외 시 기본값
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
     * RecommendationResponse로 변환
     */
    private RecommendationResponse convertToRecommendationResponse(PostPriorityScore priorityScore) {
        Post post = priorityScore.getPost();

        return RecommendationResponse.builder()
                .postId(post.getPostId())
                .productName(post.getProductName())
                .companyName(post.getCompanyName())
                .postType(post.getPostType())
                .deadline(post.getDeadline())
                .summary(post.getSummary())
                .imageUrl(post.getImageUrl())
                .interestScore(priorityScore.getInterestScore()) // <- 실제 관심도 점수
                .recommendationReason(generateRecommendationReason(priorityScore))
                .build();
    }

    /**
     * 추천 이유 생성 (우선순위 기반)
     */
    private String generateRecommendationReason(PostPriorityScore score) {
        List<String> reasons = new ArrayList<>();

        // 각 스코어 기준에 따라 이유 추가
        if (score.getInterestScore() > 70) {
            reasons.add("관심도가 높은");
        } else if (score.getInterestScore() > 30) {
            reasons.add("관심을 보인");
        }

        if (score.getRegionScore() > 80) {
            reasons.add("지역 완전 맞춤");
        } else if (score.getRegionScore() > 50) {
            reasons.add("지역 관련");
        }

        if (score.getAgeScore() > 80) {
            reasons.add("연령 적합");
        } else if (score.getAgeScore() > 50) {
            reasons.add("연령대 관련");
        }

        if (score.getRecencyScore() > 90) {
            reasons.add("최신");
        } else if (score.getRecencyScore() > 70) {
            reasons.add("신규");
        }

        // 이유가 없으면 기본값
        if (reasons.isEmpty()) {
            // 가장 높은 스코어를 기준으로 기본 이유 생성
            double maxScore = Math.max(Math.max(score.getRegionScore(), score.getAgeScore()),
                    Math.max(score.getInterestScore(), score.getRecencyScore()));

            if (maxScore == score.getRegionScore()) {
                reasons.add("지역 추천");
            } else if (maxScore == score.getAgeScore()) {
                reasons.add("맞춤 추천");
            } else if (maxScore == score.getRecencyScore()) {
                reasons.add("신규");
            } else {
                reasons.add("추천");
            }
        }

        return String.join(" ", reasons) + " 상품";
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
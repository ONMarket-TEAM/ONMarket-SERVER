package com.onmarket.recommendation.service;

import com.onmarket.business.domain.Business;
import com.onmarket.fssdata.repository.CreditLoanProductRepository;
import com.onmarket.loandata.repository.LoanProductRepository;
import com.onmarket.post.domain.Post;
import com.onmarket.post.repository.PostRepository;
import com.onmarket.supportsdata.repository.SupportProductRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RegionFilterService {

    private final PostRepository postRepository;
    private final LoanProductRepository loanProductRepository;
    private final SupportProductRepository supportProductRepository;
    private final CreditLoanProductRepository creditLoanProductRepository;

    /**
     * 🔥 최적화된 지역 기반 게시물 필터링
     */
    public List<Post> filterPostsByRegion(String sidoName, String sigunguName) {
        List<Post> filteredPosts = new ArrayList<>();

        // 1. 🚀 LoanProduct 최적화된 지역 필터링
        filteredPosts.addAll(filterLoanProductsByRegionOptimized(sidoName, sigunguName));

        // 2. 🚀 SupportProduct 최적화된 지역 필터링
        filteredPosts.addAll(filterSupportProductsByRegionOptimized(sidoName, sigunguName));

        // 3. CreditLoanProduct는 전국 단위로 모두 포함
        filteredPosts.addAll(getAllCreditLoanPosts());

        log.info("지역 필터링 결과: {} {} -> {} 개 게시물", sidoName, sigunguName, filteredPosts.size());
        return filteredPosts;
    }

    /**
     * 지역 우선순위 스코어 계산 (PriorityRecommendationService에서 사용)
     */
    public double calculateRegionPriorityScore(Business business, Post post) {
        if (business == null || business.getSidoName() == null) {
            return 50.0; // 기본 점수
        }

        try {
            // RegionFilterService를 사용하여 지역 매칭 확인
            List<Post> regionFilteredPosts = filterPostsByRegion(
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
     * 🚀 최적화된 LoanProduct 지역 필터링 (단일 쿼리)
     */
    private List<Post> filterLoanProductsByRegionOptimized(String sidoName, String sigunguName) {

        try {
            // 🔥 단일 쿼리로 모든 조건 처리
            List<Long> loanProductIds = loanProductRepository.findIdsByRegionPatterns(sidoName, sigunguName);

            if (loanProductIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 중복 제거
            Set<Long> uniqueIds = new HashSet<>(loanProductIds);
            return postRepository.findBySourceTableAndSourceIdIn("LoanProduct", uniqueIds);

        } catch (Exception e) {
            log.error("LoanProduct 지역 필터링 실패: {} {}", sidoName, sigunguName, e);
            return Collections.emptyList();
        }
    }

    /**
     * 🚀 최적화된 SupportProduct 지역 필터링 (단일 쿼리)
     */
    private List<Post> filterSupportProductsByRegionOptimized(String sidoName, String sigunguName) {
        try {
            // 🔥 단일 쿼리로 모든 조건 처리
            List<Long> supportProductIds = supportProductRepository.findIdsByRegionPatterns(sidoName, sigunguName);

            if (supportProductIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 중복 제거
            Set<Long> uniqueIds = new HashSet<>(supportProductIds);
            return postRepository.findBySourceTableAndSourceIdIn("SupportProduct", uniqueIds);

        } catch (Exception e) {
            log.error("SupportProduct 지역 필터링 실패: {} {}", sidoName, sigunguName, e);
            return Collections.emptyList();
        }
    }

    /**
     * CreditLoanProduct 전체 조회 (전국 단위)
     */
    private List<Post> getAllCreditLoanPosts() {
        try {
            return postRepository.findBySourceTable("CreditLoanProduct");
        } catch (Exception e) {
            log.error("CreditLoanProduct 조회 실패", e);
            return Collections.emptyList();
        }
    }

    /**
     * 지역명 패턴 생성
     */
    private List<String> createRegionPatterns(String sidoName, String sigunguName) {
        List<String> patterns = new ArrayList<>();

        if (sidoName != null && !sidoName.trim().isEmpty()) {
            patterns.add(sidoName); // "경기도"

            // 도/시 제거
            if (sidoName.endsWith("도") || sidoName.endsWith("시")) {
                patterns.add(sidoName.substring(0, sidoName.length() - 1)); // "경기"
            }

            // 특별시/광역시 처리
            if (sidoName.endsWith("특별시")) {
                patterns.add(sidoName.replace("특별시", "")); // "서울"
            } else if (sidoName.endsWith("광역시")) {
                patterns.add(sidoName.replace("광역시", "")); // "부산"
            }
        }

        if (sigunguName != null && !sigunguName.trim().isEmpty()) {
            patterns.add(sigunguName); // "성남시"

            // 시/구/군 제거
            if (sigunguName.endsWith("시") || sigunguName.endsWith("구") || sigunguName.endsWith("군")) {
                patterns.add(sigunguName.substring(0, sigunguName.length() - 1)); // "성남"
            }
        }

        return patterns.stream()
                .filter(pattern -> pattern != null && !pattern.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}
package com.onmarket.recommendation.service;

import com.onmarket.loandata.domain.LoanProduct;
import com.onmarket.loandata.repository.LoanProductRepository;
import com.onmarket.member.domain.Member;
import com.onmarket.post.domain.Post;
import com.onmarket.supportsdata.domain.SupportCondition;
import com.onmarket.supportsdata.domain.SupportProduct;
import com.onmarket.supportsdata.repository.SupportProductRepository;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AgeFilterService {

    private final LoanProductRepository loanProductRepository;
    private final SupportProductRepository supportProductRepository;

    /**
     * 연령 기반 게시물 필터링
     */
    public List<Post> filterPostsByAge(LocalDate birthDate, List<Post> posts) {
        if (birthDate == null) {
            return posts; // 나이 정보가 없으면 모든 게시물 반환
        }

        int age = calculateAge(birthDate);

        return posts.stream()
                .filter(post -> isAgeEligible(post, age))
                .collect(Collectors.toList());
    }

    /**
     * 연령 우선순위 스코어 계산 (PriorityRecommendationService에서 사용)
     */
    public double calculateAgePriorityScore(Member member, Post post) {
        if (member == null || member.getBirthDate() == null) {
            return 50.0; // 기본 점수
        }

        int age = calculateAge(member.getBirthDate());

        if (isAgeEligible(post, age)) {
            return 100.0; // 완전 적합
        } else {
            return 20.0; // 부적합하지만 최소 점수 부여
        }
    }

    /**
     * 특정 게시물이 연령 조건에 맞는지 확인
     */
    private boolean isAgeEligible(Post post, int age) {
        switch (post.getSourceTable()) {
            case "LoanProduct":
                return checkLoanProductAge(post.getSourceId(), age);
            case "SupportProduct":
                return checkSupportProductAge(post.getSourceId(), age);
            case "CreditLoanProduct":
                return age >= 19; // 성인만 가능하다고 가정
            default:
                return true;
        }
    }

    /**
     * LoanProduct 연령 확인
     */
    private boolean checkLoanProductAge(Long productId, int age) {
        LoanProduct product = loanProductRepository.findById(productId).orElse(null);
        if (product == null) return false;

        // age_39_blw 필드 확인 (39세 이하만 가능한 상품)
        if ("Y".equals(product.getAgeBelow39()) && age > 39) {
            return false;
        }

        // age 필드에서 연령 조건 파싱
        String ageCondition = product.getAge();
        if (ageCondition != null && !ageCondition.trim().isEmpty()) {
            return parseAgeCondition(ageCondition, age);
        }

        return true; // 연령 조건이 없으면 모든 연령 가능
    }

    /**
     * SupportProduct 연령 확인
     */
    private boolean checkSupportProductAge(Long productId, int age) {
        SupportProduct product = supportProductRepository.findById(productId).orElse(null);
        if (product == null) return false;

        SupportCondition condition = product.getSupportCondition();
        if (condition == null) return true;

        Integer ageStart = condition.getAgeStart();
        Integer ageEnd = condition.getAgeEnd();

        if (ageStart != null && age < ageStart) return false;
        if (ageEnd != null && age > ageEnd) return false;

        return true;
    }

    /**
     * 나이 계산
     */
    private int calculateAge(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * 연령 조건 문자열 파싱
     */
    private boolean parseAgeCondition(String ageCondition, int age) {
        try {
            // "만 19세 이상" 같은 형태 파싱
            if (ageCondition.contains("이상")) {
                String numberStr = ageCondition.replaceAll("[^0-9]", "");
                if (!numberStr.isEmpty()) {
                    int minAge = Integer.parseInt(numberStr);
                    return age >= minAge;
                }
            }

            // "만 19세~64세" 같은 형태 파싱
            if (ageCondition.contains("~") || ageCondition.contains("-")) {
                String[] parts = ageCondition.split("[~-]");
                if (parts.length == 2) {
                    String minStr = parts[0].replaceAll("[^0-9]", "");
                    String maxStr = parts[1].replaceAll("[^0-9]", "");

                    if (!minStr.isEmpty() && !maxStr.isEmpty()) {
                        int minAge = Integer.parseInt(minStr);
                        int maxAge = Integer.parseInt(maxStr);
                        return age >= minAge && age <= maxAge;
                    }
                }
            }

            return true; // 파싱할 수 없으면 허용
        } catch (Exception e) {
            log.warn("연령 조건 파싱 실패: {}", ageCondition);
            return true;
        }
    }
}
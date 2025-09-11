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
     * 🔥 연령 기반 게시물 필터링 제거 - 이제 점수로만 반영
     * @deprecated 연령 필터링 대신 점수 기반 추천으로 변경됨
     */
    @Deprecated
    public List<Post> filterPostsByAge(LocalDate birthDate, List<Post> posts) {
        // 🔥 필터링하지 않고 모든 게시물 반환 (점수로 처리)
        log.debug("연령 필터링 제거됨 - 모든 게시물 반환: {}건", posts.size());
        return posts;
    }

    /**
     * 🔥 개선된 연령 우선순위 스코어 계산 (세분화된 점수 체계)
     */
    public double calculateAgePriorityScore(Member member, Post post) {
        if (member == null || member.getBirthDate() == null) {
            return 70.0; // 나이 정보 없으면 중간 점수 (50 -> 70으로 상향)
        }

        int age = calculateAge(member.getBirthDate());

        // 🔥 연령 적합성에 따른 세분화된 점수
        AgeCompatibility compatibility = evaluateAgeCompatibility(post, age);

        double score = switch (compatibility) {
            case PERFECT_MATCH -> 100.0;      // 완벽한 연령 매칭
            case GOOD_MATCH -> 85.0;          // 좋은 연령 매칭
            case ACCEPTABLE -> 70.0;          // 수용 가능한 연령
            case MARGINAL -> 50.0;            // 경계선상 연령
            case POOR_MATCH -> 30.0;          // 부적합하지만 최소 점수
            case EXCLUDED -> 15.0;            // 명시적 제외 대상이지만 최소 점수
        };

        log.debug("연령 점수 계산: 나이={}, 적합성={}, 점수={:.1f} (PostId={})",
                age, compatibility, score, post.getPostId());

        return score;
    }

    /**
     * 🔥 연령 적합성 평가 (세분화된 등급)
     */
    private AgeCompatibility evaluateAgeCompatibility(Post post, int age) {
        switch (post.getSourceTable()) {
            case "LoanProduct":
                return evaluateLoanProductAgeCompatibility(post.getSourceId(), age);
            case "SupportProduct":
                return evaluateSupportProductAgeCompatibility(post.getSourceId(), age);
            case "CreditLoanProduct":
                return evaluateCreditLoanAgeCompatibility(age);
            default:
                return AgeCompatibility.ACCEPTABLE; // 기본값
        }
    }

    /**
     * 🔥 대출상품 연령 적합성 평가
     */
    private AgeCompatibility evaluateLoanProductAgeCompatibility(Long productId, int age) {
        try {
            LoanProduct product = loanProductRepository.findById(productId).orElse(null);
            if (product == null) {
                return AgeCompatibility.ACCEPTABLE;
            }

            // 39세 이하 전용 상품 체크
            if ("Y".equals(product.getAgeBelow39())) {
                if (age <= 35) {
                    return AgeCompatibility.PERFECT_MATCH; // 35세 이하는 완벽
                } else if (age <= 39) {
                    return AgeCompatibility.GOOD_MATCH;    // 36-39세는 좋음
                } else if (age <= 42) {
                    return AgeCompatibility.POOR_MATCH;    // 40-42세는 부적합하지만 점수 부여
                } else {
                    return AgeCompatibility.EXCLUDED;      // 43세 이상은 최소 점수
                }
            }

            // 일반 연령 조건 파싱
            String ageCondition = product.getAge();
            if (ageCondition != null && !ageCondition.trim().isEmpty()) {
                return parseAgeConditionForCompatibility(ageCondition, age);
            }

            // 연령 조건이 없으면 나이대별 기본 점수
            return getDefaultAgeCompatibility(age);

        } catch (Exception e) {
            log.warn("대출상품 연령 평가 실패: productId={}", productId, e);
            return AgeCompatibility.ACCEPTABLE;
        }
    }

    /**
     * 🔥 지원사업 연령 적합성 평가
     */
    private AgeCompatibility evaluateSupportProductAgeCompatibility(Long productId, int age) {
        try {
            SupportProduct product = supportProductRepository.findById(productId).orElse(null);
            if (product == null) {
                return AgeCompatibility.ACCEPTABLE;
            }

            SupportCondition condition = product.getSupportCondition();
            if (condition == null) {
                return getDefaultAgeCompatibility(age);
            }

            Integer ageStart = condition.getAgeStart();
            Integer ageEnd = condition.getAgeEnd();

            // 연령 범위가 지정된 경우
            if (ageStart != null && ageEnd != null) {
                return evaluateAgeRange(age, ageStart, ageEnd);
            } else if (ageStart != null) {
                // 최소 연령만 지정된 경우
                return evaluateMinAge(age, ageStart);
            } else if (ageEnd != null) {
                // 최대 연령만 지정된 경우
                return evaluateMaxAge(age, ageEnd);
            }

            return getDefaultAgeCompatibility(age);

        } catch (Exception e) {
            log.warn("지원사업 연령 평가 실패: productId={}", productId, e);
            return AgeCompatibility.ACCEPTABLE;
        }
    }

    /**
     * 🔥 신용대출 연령 적합성 평가
     */
    private AgeCompatibility evaluateCreditLoanAgeCompatibility(int age) {
        if (age < 19) {
            return AgeCompatibility.EXCLUDED;      // 미성년자
        } else if (age >= 19 && age <= 35) {
            return AgeCompatibility.PERFECT_MATCH; // 젊은 성인
        } else if (age >= 36 && age <= 50) {
            return AgeCompatibility.GOOD_MATCH;    // 중년
        } else if (age >= 51 && age <= 65) {
            return AgeCompatibility.ACCEPTABLE;    // 장년
        } else {
            return AgeCompatibility.MARGINAL;      // 고령
        }
    }

    /**
     * 🔥 연령 범위 평가
     */
    private AgeCompatibility evaluateAgeRange(int age, int ageStart, int ageEnd) {
        if (age < ageStart) {
            int gap = ageStart - age;
            if (gap <= 2) {
                return AgeCompatibility.MARGINAL;  // 2세 이내 차이
            } else if (gap <= 5) {
                return AgeCompatibility.POOR_MATCH; // 5세 이내 차이
            } else {
                return AgeCompatibility.EXCLUDED;   // 5세 초과 차이
            }
        } else if (age > ageEnd) {
            int gap = age - ageEnd;
            if (gap <= 2) {
                return AgeCompatibility.MARGINAL;   // 2세 이내 초과
            } else if (gap <= 5) {
                return AgeCompatibility.POOR_MATCH; // 5세 이내 초과
            } else {
                return AgeCompatibility.EXCLUDED;   // 5세 초과
            }
        } else {
            // 범위 내에 있는 경우 - 중앙에 가까울수록 높은 점수
            int rangeSize = ageEnd - ageStart;
            int centerAge = (ageStart + ageEnd) / 2;
            int distanceFromCenter = Math.abs(age - centerAge);

            if (rangeSize <= 5) {
                return AgeCompatibility.PERFECT_MATCH; // 작은 범위는 모두 완벽
            } else if (distanceFromCenter <= rangeSize / 4) {
                return AgeCompatibility.PERFECT_MATCH; // 중앙 25% 범위
            } else if (distanceFromCenter <= rangeSize / 2) {
                return AgeCompatibility.GOOD_MATCH;    // 중앙 50% 범위
            } else {
                return AgeCompatibility.ACCEPTABLE;    // 범위 내 나머지
            }
        }
    }

    /**
     * 최소 연령 평가
     */
    private AgeCompatibility evaluateMinAge(int age, int minAge) {
        if (age < minAge) {
            int gap = minAge - age;
            if (gap <= 2) return AgeCompatibility.MARGINAL;
            else if (gap <= 5) return AgeCompatibility.POOR_MATCH;
            else return AgeCompatibility.EXCLUDED;
        } else {
            // 최소 연령 이상인 경우 나이대별 기본 점수
            return getDefaultAgeCompatibility(age);
        }
    }

    /**
     * 최대 연령 평가
     */
    private AgeCompatibility evaluateMaxAge(int age, int maxAge) {
        if (age > maxAge) {
            int gap = age - maxAge;
            if (gap <= 2) return AgeCompatibility.MARGINAL;
            else if (gap <= 5) return AgeCompatibility.POOR_MATCH;
            else return AgeCompatibility.EXCLUDED;
        } else {
            return getDefaultAgeCompatibility(age);
        }
    }

    /**
     * 🔥 나이대별 기본 적합성 (연령 조건이 없는 경우)
     */
    private AgeCompatibility getDefaultAgeCompatibility(int age) {
        if (age >= 25 && age <= 45) {
            return AgeCompatibility.PERFECT_MATCH; // 주력 경제활동 연령
        } else if (age >= 20 && age <= 55) {
            return AgeCompatibility.GOOD_MATCH;    // 일반 경제활동 연령
        } else if (age >= 18 && age <= 65) {
            return AgeCompatibility.ACCEPTABLE;    // 확장 경제활동 연령
        } else if (age >= 16 && age <= 70) {
            return AgeCompatibility.MARGINAL;      // 경계 연령
        } else {
            return AgeCompatibility.POOR_MATCH;    // 일반적이지 않은 연령
        }
    }

    /**
     * 🔥 연령 조건 문자열을 적합성으로 파싱
     */
    private AgeCompatibility parseAgeConditionForCompatibility(String ageCondition, int age) {
        try {
            // "만 19세 이상" 형태 파싱
            if (ageCondition.contains("이상")) {
                String numberStr = ageCondition.replaceAll("[^0-9]", "");
                if (!numberStr.isEmpty()) {
                    int minAge = Integer.parseInt(numberStr);
                    return evaluateMinAge(age, minAge);
                }
            }

            // "만 19세~64세" 형태 파싱
            if (ageCondition.contains("~") || ageCondition.contains("-")) {
                String[] parts = ageCondition.split("[~-]");
                if (parts.length == 2) {
                    String minStr = parts[0].replaceAll("[^0-9]", "");
                    String maxStr = parts[1].replaceAll("[^0-9]", "");

                    if (!minStr.isEmpty() && !maxStr.isEmpty()) {
                        int minAge = Integer.parseInt(minStr);
                        int maxAge = Integer.parseInt(maxStr);
                        return evaluateAgeRange(age, minAge, maxAge);
                    }
                }
            }

            // "만 39세 이하" 형태 파싱
            if (ageCondition.contains("이하")) {
                String numberStr = ageCondition.replaceAll("[^0-9]", "");
                if (!numberStr.isEmpty()) {
                    int maxAge = Integer.parseInt(numberStr);
                    return evaluateMaxAge(age, maxAge);
                }
            }

            return AgeCompatibility.ACCEPTABLE; // 파싱할 수 없으면 수용 가능

        } catch (Exception e) {
            log.warn("연령 조건 파싱 실패: {}", ageCondition, e);
            return AgeCompatibility.ACCEPTABLE;
        }
    }

    /**
     * 나이 계산
     */
    private int calculateAge(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * 🔥 연령 적합성 등급 열거형
     */
    public enum AgeCompatibility {
        PERFECT_MATCH("완벽한 연령 매칭"),
        GOOD_MATCH("좋은 연령 매칭"),
        ACCEPTABLE("수용 가능한 연령"),
        MARGINAL("경계선상 연령"),
        POOR_MATCH("부적합하지만 고려 가능"),
        EXCLUDED("명시적 제외 대상");

        private final String description;

        AgeCompatibility(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
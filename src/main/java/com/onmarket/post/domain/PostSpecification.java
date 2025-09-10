package com.onmarket.post.domain; // Post 엔티티와 같은 패키지 또는 적절한 위치에 생성

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;

public class PostSpecification {

    public static Specification<Post> search(PostType postType, String keyword, String companyName) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. PostType 조건은 항상 적용
            predicates.add(criteriaBuilder.equal(root.get("postType"), postType));

            // 2. 키워드가 있으면 productName 또는 companyName에서 검색
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                Predicate productNameLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), pattern);
                Predicate companyNameLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("companyName")), pattern);
                predicates.add(criteriaBuilder.or(productNameLike, companyNameLike));
            }

            // 3. 회사명 필터가 있으면 companyName에서 검색
            if (companyName != null && !companyName.trim().isEmpty()) {
                String pattern = "%" + companyName.toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("companyName")), pattern));
            }

            // 4. 생성된 모든 조건을 AND로 연결
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
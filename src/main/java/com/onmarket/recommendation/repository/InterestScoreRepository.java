package com.onmarket.recommendation.repository;

import com.onmarket.business.domain.Business;
import com.onmarket.member.domain.Member;
import com.onmarket.post.domain.Post;
import com.onmarket.recommendation.domain.InterestScore;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InterestScoreRepository extends JpaRepository<InterestScore, Long> {


    Optional<InterestScore> findByMemberAndBusinessAndPost(Member member, Business business, Post post);

    /**
     * 상위 추천 조회 (totalScore 기준)
     */
    @Query("SELECT is FROM InterestScore is " +
            "JOIN FETCH is.post p " +
            "WHERE is.member = :member AND is.business = :business " +
            "AND is.totalScore >= 0 " +  // 0점도 포함
            "ORDER BY is.totalScore DESC, p.createdAt DESC")
    List<InterestScore> findTopRecommendationsByMemberAndBusiness(
            @Param("member") Member member,
            @Param("business") Business business,
            Pageable pageable);

    default List<InterestScore> findTopRecommendationsByMemberAndBusiness(
            Member member, Business business, int limit) {
        return findTopRecommendationsByMemberAndBusiness(
                member, business, PageRequest.of(0, limit));
    }

    /**
     * 사용자별 모든 추천 점수 조회
     */
    @Query("SELECT is FROM InterestScore is " +
            "WHERE is.member = :member AND is.business = :business " +
            "ORDER BY is.totalScore DESC")
    List<InterestScore> findByMemberAndBusinessOrderByTotalScoreDesc(
            @Param("member") Member member,
            @Param("business") Business business);

    /**
     * 특정 점수 이상의 추천만 조회
     */
    @Query("SELECT is FROM InterestScore is " +
            "JOIN FETCH is.post p " +
            "WHERE is.member = :member AND is.business = :business " +
            "AND is.totalScore >= :minScore " +
            "ORDER BY is.totalScore DESC")
    List<InterestScore> findByMemberAndBusinessAndMinScore(
            @Param("member") Member member,
            @Param("business") Business business,
            @Param("minScore") Double minScore);

    /**
     * 🆕 추가: 상호작용 없는 사용자를 위한 초기 추천 생성
     */
    @Query("SELECT is FROM InterestScore is " +
            "JOIN FETCH is.post p " +
            "WHERE is.member = :member AND is.business = :business " +
            "ORDER BY p.createdAt DESC")  // totalScore 조건 제거, 최신순으로 정렬
    List<InterestScore> findAllByMemberAndBusinessOrderByCreatedAt(
            @Param("member") Member member,
            @Param("business") Business business,
            Pageable pageable);
}

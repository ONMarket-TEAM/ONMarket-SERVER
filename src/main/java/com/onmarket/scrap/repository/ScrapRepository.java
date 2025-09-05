package com.onmarket.scrap.repository;

import com.onmarket.member.domain.Member;
import com.onmarket.post.domain.Post;
import com.onmarket.scrap.domain.Scrap;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {

    /**
     * ID만으로 스크랩 존재 여부 확인 (성능 최적화)
     */
    boolean existsByMemberMemberIdAndPostPostId(Long memberId, Long postId);

    /**
     * 엔티티로 스크랩 찾기 (토글할 때 사용)
     */
    Optional<Scrap> findByMemberAndPost(Member member, Post post);

    /**
     * 내 스크랩 목록 조회 (N+1 문제 방지)
     */
    @Query("SELECT s FROM Scrap s " +
            "JOIN FETCH s.post p " +
            "WHERE s.member = :member " +
            "ORDER BY s.createdAt DESC")
    List<Scrap> findByMemberOrderByCreatedAtDesc(@Param("member") Member member);

    /**
     * 엔티티로 스크랩 개수 조회 (토글할 때 사용)
     */
    Long countByPost(Post post);

    /**
     * ID만으로 스크랩 개수 조회 (성능 최적화)
     */
    @Query("SELECT COUNT(s) FROM Scrap s WHERE s.post.postId = :postId")
    Long countByPostId(@Param("postId") Long postId);
}
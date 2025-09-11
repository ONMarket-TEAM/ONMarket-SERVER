package com.onmarket.recommendation.repository;

import com.onmarket.member.domain.Member;
import com.onmarket.post.domain.Post;
import com.onmarket.recommendation.domain.InteractionType;
import com.onmarket.recommendation.domain.UserInteraction;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {

    List<UserInteraction> findByMemberAndPost(Member member, Post post);

    // 최적화: 한 번의 쿼리로 모든 상호작용 데이터 조회
    @Query("SELECT ui FROM UserInteraction ui " +
            "JOIN FETCH ui.post p " +
            "WHERE ui.member = :member")
    List<UserInteraction> findByMemberWithPosts(@Param("member") Member member);

    // 특정 기간 내 상호작용 조회
    @Query("SELECT ui FROM UserInteraction ui " +
            "WHERE ui.member = :member " +
            "AND ui.createdAt >= :fromDate")
    List<UserInteraction> findByMemberSince(@Param("member") Member member,
                                            @Param("fromDate") LocalDateTime fromDate);
    /**
     * 🔥 특정 회원의 최근 중요한 상호작용 조회
     */
    List<UserInteraction> findByMemberAndCreatedAtAfterAndInteractionTypeIn(
            Member member,
            LocalDateTime dateTime,
            List<InteractionType> interactionTypes);

    /**
     * 🔥 특정 게시물의 최근 상호작용 조회
     */
    List<UserInteraction> findByPostAndCreatedAtAfter(Post post, LocalDateTime dateTime);

    /**
     * 🔥 특정 게시물의 모든 상호작용 조회
     */
    List<UserInteraction> findByPost(Post post);

    /**
     * 🔥 특정 회원의 모든 상호작용 조회 (최신순)
     */
    List<UserInteraction> findByMemberOrderByCreatedAtDesc(Member member);

    List<UserInteraction> findByMemberAndCreatedAtAfterOrderByCreatedAtDesc(Member member, LocalDateTime threeMonthsAgo);
    List<UserInteraction> findByMemberAndPostAndCreatedAtAfter(Member member, Post post, LocalDateTime dateTime);

}

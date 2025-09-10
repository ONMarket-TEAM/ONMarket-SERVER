package com.onmarket.notification.repository;

import com.onmarket.member.domain.Member;
import com.onmarket.notification.domain.NotificationHistory;
import com.onmarket.notification.domain.NotificationType;
import com.onmarket.post.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {

    // 회원별 알림 목록 조회 (최신순, 페이징)
    @Query("SELECT nh FROM NotificationHistory nh " +
            "JOIN FETCH nh.post p " +
            "WHERE nh.member = :member " +
            "ORDER BY nh.createdAt DESC")
    Page<NotificationHistory> findByMemberOrderByCreatedAtDesc(@Param("member") Member member, Pageable pageable);

    // 회원별 읽지 않은 알림 개수 조회
    Long countByMemberAndIsReadFalse(Member member);

    // 모든 알림 읽음 처리
    @Modifying
    @Query("UPDATE NotificationHistory nh SET nh.isRead = true " +
            "WHERE nh.member = :member AND nh.isRead = false")
    int markAllAsReadByMember(@Param("member") Member member);

    // 중복 알림 방지 체크
    @Query("SELECT nh FROM NotificationHistory nh " +
            "WHERE nh.member = :member " +
            "AND nh.post = :post " +
            "AND nh.notificationType = :type " +
            "AND DATE(nh.createdAt) = CURRENT_DATE")
    Optional<NotificationHistory> findTodayNotification(@Param("member") Member member,
                                                        @Param("post") Post post,
                                                        @Param("type") NotificationType type);
}

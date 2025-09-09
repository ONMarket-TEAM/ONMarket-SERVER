package com.onmarket.notification.repository;

import com.onmarket.member.domain.Member;
import com.onmarket.notification.domain.NotificationSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
 import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationSubscriptionRepository extends JpaRepository<NotificationSubscription, Long> {

    // 회원 ID로 구독 정보 조회
    Optional<NotificationSubscription> findByMemberMemberId(Long memberId);

    // 특정 회원들의 활성 구독 정보 조회 (스크랩한 사용자들에게 알림 발송용)
    @Query("SELECT ns FROM NotificationSubscription ns " +
            "JOIN FETCH ns.member m " +
            "WHERE ns.member IN :members AND ns.isActive = true")
    List<NotificationSubscription> findActiveSubscriptionsByMembers(@Param("members") List<Member> members);

    // endpoint로 구독 정보 찾기 (중복 구독 체크용)
    Optional<NotificationSubscription> findByEndpoint(String endpoint);
}

package com.onmarket.notification.service;

import com.onmarket.notification.dto.NotificationListResponse;
import com.onmarket.notification.dto.NotificationSubscriptionRequest;
import com.onmarket.notification.dto.NotificationSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    // 구독 관리
    void subscribe(String email, NotificationSubscriptionRequest request);
    void unsubscribe(String email);
    boolean isSubscribed(String email);

    // 알림 목록 조회
    Page<NotificationListResponse> getNotifications(String email, Pageable pageable);

    // 읽지 않은 알림 개수 조회
    NotificationSummaryResponse getNotificationSummary(String email);

    // 모든 알림 읽기
    int markAllAsRead(String email);

    // 개별 알림 읽기
    void markAsRead(String email, Long notificationId);

    // 마감일 알림 생성 (기능 1 - 스케줄러에서 호출)
    void createDeadlineNotifications();

    // 테스트용 알림 전송
    void sendTestNotification();
}

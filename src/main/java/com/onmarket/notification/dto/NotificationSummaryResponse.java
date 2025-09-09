package com.onmarket.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSummaryResponse {

    // 읽지 않은 알림 개수
    private Long unreadCount;

    // 푸시 알림 구독 상태
    private Boolean isSubscribed;

    // 읽지 않은 알림이 있는지 여부
    public Boolean hasUnreadNotifications() {
        return unreadCount > 0;
    }

    // 정적 팩토리 메서드
    public static NotificationSummaryResponse of(Long unreadCount, Boolean isSubscribed) {
        return NotificationSummaryResponse.builder()
                .unreadCount(unreadCount)
                .isSubscribed(isSubscribed)
                .build();
    }
}

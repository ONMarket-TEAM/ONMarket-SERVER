package com.onmarket.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSubscriptionResponse {

    // 현재 구독 상태
    private Boolean isSubscribed;

    public static NotificationSubscriptionResponse of(boolean isSubscribed) {
        return NotificationSubscriptionResponse.builder()
                .isSubscribed(isSubscribed)
                .build();
    }
}

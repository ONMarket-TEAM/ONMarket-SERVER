package com.onmarket.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationMarkAllReadResponse {

    // 읽음 처리된 알림 개수
    private Integer markedCount;

    public static NotificationMarkAllReadResponse of(int count) {
        return NotificationMarkAllReadResponse.builder()
                .markedCount(count)
                .build();
    }
}

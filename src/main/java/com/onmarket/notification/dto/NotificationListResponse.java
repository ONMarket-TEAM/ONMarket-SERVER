package com.onmarket.notification.dto;

import com.onmarket.notification.domain.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationListResponse {

    private Long notificationId;
    private String title;
    private String message;
    private NotificationType notificationType;

    // 관련 게시물 정보
    private Long postId;
    private String productName;

    // 읽음 상태
    private Boolean isRead;

    // 생성 시간
    private LocalDateTime createdAt;

    // 상대 시간 표시 (예: "2시간 전", "1일 전")
    public String getRelativeTime() {
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(createdAt, now).toMinutes();

        if (minutes < 1) return "방금 전";
        if (minutes < 60) return minutes + "분 전";

        long hours = minutes / 60;
        if (hours < 24) return hours + "시간 전";

        long days = hours / 24;
        if (days < 30) return days + "일 전";

        long months = days / 30;
        if (months < 12) return months + "개월 전";

        long years = months / 12;
        return years + "년 전";
    }
}

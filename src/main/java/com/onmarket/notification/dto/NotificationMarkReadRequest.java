package com.onmarket.notification.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationMarkReadRequest {

    @NotNull(message = "알림 ID는 필수입니다")
    private Long notificationId;
}

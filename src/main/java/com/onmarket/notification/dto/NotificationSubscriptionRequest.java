package com.onmarket.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSubscriptionRequest {

    @NotBlank(message = "endpoint는 필수입니다")
    private String endpoint;

    @NotBlank(message = "p256dh key는 필수입니다")
    private String p256dhKey;

    @NotBlank(message = "auth key는 필수입니다")
    private String authKey;
}
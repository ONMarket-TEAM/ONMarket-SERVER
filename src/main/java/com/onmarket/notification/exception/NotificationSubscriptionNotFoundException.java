package com.onmarket.notification.exception;

public class NotificationSubscriptionNotFoundException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "알림 구독 정보를 찾을 수 없습니다.";

    public NotificationSubscriptionNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public NotificationSubscriptionNotFoundException(String message) {
        super(message);
    }

    public NotificationSubscriptionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public static NotificationSubscriptionNotFoundException forUser(String email) {
        return new NotificationSubscriptionNotFoundException(
                String.format("사용자 %s의 구독 정보를 찾을 수 없습니다.", email)
        );
    }
}

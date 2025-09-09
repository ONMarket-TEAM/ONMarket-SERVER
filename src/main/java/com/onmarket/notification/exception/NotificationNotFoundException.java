package com.onmarket.notification.exception;

public class NotificationNotFoundException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "알림을 찾을 수 없습니다.";

    public NotificationNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public NotificationNotFoundException(String message) {
        super(message);
    }

    public NotificationNotFoundException(Long notificationId) {
        super(String.format("ID가 %d인 알림을 찾을 수 없습니다.", notificationId));
    }

    public NotificationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

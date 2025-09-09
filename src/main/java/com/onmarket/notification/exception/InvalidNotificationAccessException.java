package com.onmarket.notification.exception;

public class InvalidNotificationAccessException extends RuntimeException {

  private static final String DEFAULT_MESSAGE = "해당 알림에 접근할 권한이 없습니다.";

  public InvalidNotificationAccessException() {
    super(DEFAULT_MESSAGE);
  }

  public InvalidNotificationAccessException(String message) {
    super(message);
  }

  public InvalidNotificationAccessException(Long notificationId, String userEmail) {
    super(String.format("사용자 %s는 알림 %d에 접근할 권한이 없습니다.", userEmail, notificationId));
  }

  public InvalidNotificationAccessException(String message, Throwable cause) {
    super(message, cause);
  }
}

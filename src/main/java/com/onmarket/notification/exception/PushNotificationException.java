package com.onmarket.notification.exception;

public class PushNotificationException extends RuntimeException {

  private static final String DEFAULT_MESSAGE = "푸시 알림 발송에 실패했습니다.";

  public PushNotificationException() {
    super(DEFAULT_MESSAGE);
  }

  public PushNotificationException(String message) {
    super(message);
  }

  public PushNotificationException(String message, Throwable cause) {
    super(message, cause);
  }

  public PushNotificationException(Throwable cause) {
    super(DEFAULT_MESSAGE, cause);
  }
}

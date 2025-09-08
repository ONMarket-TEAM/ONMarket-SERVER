package com.onmarket.notification.domain;

public enum NotificationType {
    DEADLINE_D3("마감 3일 전"),
    DEADLINE_D1("마감 1일 전"),
    DEADLINE_DDAY("마감일");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    // D-day 계산에 따른 알림 타입 반환
    public static NotificationType fromDDay(long daysRemaining) {
        return switch ((int) daysRemaining) {
            case 3 -> DEADLINE_D3;
            case 1 -> DEADLINE_D1;
            case 0 -> DEADLINE_DDAY;
            default -> null;
        };
    }

    // 알림 메시지 템플릿 생성
    public String createMessage(String productName) {
        return switch (this) {
            case DEADLINE_D3 -> String.format("스크랩한 '%s' 정책이 3일 후 마감됩니다!", productName);
            case DEADLINE_D1 -> String.format("스크랩한 '%s' 정책이 내일 마감됩니다!", productName);
            case DEADLINE_DDAY -> String.format("스크랩한 '%s' 정책이 오늘 마감됩니다!", productName);
        };
    }

    // 알림 제목 템플릿 생성
    public String createTitle(String productName) {
        return switch (this) {
            case DEADLINE_D3 -> "[온마켓] 마감 3일 전";
            case DEADLINE_D1 -> "[온마켓] 마감 1일 전";
            case DEADLINE_DDAY -> "[온마켓] 오늘 마감";
        };
    }
}

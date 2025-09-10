package com.onmarket.notification.scheduler;

import com.onmarket.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final NotificationService notificationService;

    /**
     * 매일 오전 9시에 마감일 알림 생성
     * D-3, D-1, D-Day 알림을 자동으로 생성하여 사용자에게 발송
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul") // 매일 오전 9시
    public void createDailyDeadlineNotifications() {
        log.info("일일 마감일 알림 생성 스케줄러 실행 시작");

        try {
            notificationService.createDeadlineNotifications();
            log.info("일일 마감일 알림 생성 스케줄러 실행 완료");
        } catch (Exception e) {
            log.error("마감일 알림 생성 중 오류 발생", e);
        }
    }

    /**
     * 테스트용 알림
     */
    @Scheduled(cron = "0 23 0 * * *", zone = "Asia/Seoul")
    public void sendTestNotification() {
        log.info("테스트 알림 스케줄러 실행 시작");

        try {
            notificationService.sendTestNotification();
            log.info("테스트 알림 스케줄러 실행 완료");
        } catch (Exception e) {
            log.error("테스트 알림 전송 중 오류 발생", e);
        }
    }
}

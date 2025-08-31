package com.onmarket.supportsdata.scheduler;

import com.onmarket.supportsdata.service.PublicDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublicDataScheduler {

    private final PublicDataService publicDataService;

    /**
     * 매일 새벽 4시에 실행되어 정부 지원 정책 데이터를 수집합니다.
     * cron = "초 분 시 일 월 요일"
     * "0 0 4 * * *" = 매일 4시 0분 0초
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void scheduleFetchSupportProducts() {
        log.info("[스케줄러 시작] 정부 지원정책 데이터 수집을 시작합니다.");
        try {

            publicDataService.findAndSaveAllServices().block();

            log.info("[스케줄러 성공] 정부 지원정책 데이터 수집을 완료했습니다.");
        } catch (Exception e) {
            log.error("[스케줄러 오류] 정부 지원정책 데이터 수집 중 오류가 발생했습니다.", e);
        }
    }
}
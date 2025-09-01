package com.onmarket.fssdata.scheduler;

import com.onmarket.fssdata.service.CreditLoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreditLoanScheduler {

    private final CreditLoanService creditLoanService;

    // 매일 새벽 3시에 데이터 업데이트
    @Scheduled(cron = "0 0 3 * * *")
    public void updateCreditLoanData() {
        log.info("정기 데이터 업데이트 시작");
        try {
            creditLoanService.fetchAndSaveAllCreditLoanData();
            log.info("정기 데이터 업데이트 완료");
        } catch (Exception e) {
            log.error("정기 데이터 업데이트 실패: ", e);
        }
    }
}
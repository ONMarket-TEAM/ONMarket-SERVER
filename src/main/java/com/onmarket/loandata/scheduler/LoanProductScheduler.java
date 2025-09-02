package com.onmarket.loandata.scheduler;

import com.onmarket.loandata.service.LoanProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoanProductScheduler {

    private final LoanProductService loanProductService;

    // 매일 새벽 2시에 데이터 업데이트
    @Scheduled(cron = "0 0 2 * * *")
    public void updateLoanProductData() {
        log.info("정기 대출상품 데이터 업데이트 시작");
        try {
            long beforeCount = loanProductService.getTotalProductCount();
            log.info("업데이트 전 상품 수: {}", beforeCount);

            loanProductService.fetchAndSaveAllLoanProducts();

            long afterCount = loanProductService.getTotalProductCount();
            log.info("업데이트 후 상품 수: {}", afterCount);
            log.info("정기 대출상품 데이터 업데이트 완료 (변화: {}개)", afterCount - beforeCount);
        } catch (Exception e) {
            log.error("정기 대출상품 데이터 업데이트 실패: ", e);
        }
    }

    // 매주 일요일 오전 3시에 전체 데이터 정리
    @Scheduled(cron = "0 0 3 * * SUN")
    public void weeklyDataMaintenance() {
        log.info("주간 대출상품 데이터 정리 작업 시작");
        try {
            long totalProducts = loanProductService.getTotalProductCount();
            log.info("현재 총 대출상품 수: {}", totalProducts);

            // 필요시 데이터 정리 로직 추가
            // 예: 중복 데이터 제거, 오래된 데이터 정리 등

            log.info("주간 대출상품 데이터 정리 작업 완료");
        } catch (Exception e) {
            log.error("주간 대출상품 데이터 정리 작업 실패: ", e);
        }
    }

    // 매시간 데이터 상태 체크
    @Scheduled(fixedRate = 3600000) // 1시간마다
    public void hourlyHealthCheck() {
        try {
            long totalProducts = loanProductService.getTotalProductCount();
            if (totalProducts == 0) {
                log.warn("주의: 대출상품 데이터가 없습니다. 데이터 수집이 필요할 수 있습니다.");
            } else {
                log.debug("대출상품 데이터 상태 정상 - 총 {}개 상품", totalProducts);
            }
        } catch (Exception e) {
            log.error("대출상품 데이터 상태 체크 실패: ", e);
        }
    }
}
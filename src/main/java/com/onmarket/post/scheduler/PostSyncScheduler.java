package com.onmarket.post.scheduler;

import com.onmarket.post.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
//@Component
@RequiredArgsConstructor
public class PostSyncScheduler {

    private final PostService postService;

    /**
     * 매일 새벽 4시 30분에 변경된 상품 데이터를 감지하여 Post를 동기화합니다.
     * cron = "초 분 시 일 월 요일"
     */
    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    public void schedulePostSynchronization() {
        log.info("[스케줄러] 변경된 상품 데이터 Post 동기화 작업을 시작합니다.");
        try {
            postService.synchronizeModifiedPosts();
            log.info("[스케줄러] Post 동기화 작업을 성공적으로 완료했습니다.");
        } catch (Exception e) {
            log.error("[스케줄러] Post 동기화 작업 중 오류가 발생했습니다.", e);
        }
    }
}
package com.onmarket.recommendation.handler;

import com.onmarket.business.domain.Business;
import com.onmarket.business.repository.BusinessRepository;
import com.onmarket.member.domain.Member;
import com.onmarket.member.service.MemberService;
import com.onmarket.recommendation.domain.MainBusinessChangedEvent;
import com.onmarket.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendationEventHandler {

    private final RecommendationService recommendationService;
    private final MemberService memberService;
    private final BusinessRepository businessRepository;

    @EventListener
    @Async
    public void handleMainBusinessChanged(MainBusinessChangedEvent event) {
        log.info("대표 사업장 변경 이벤트 처리: {} - {} -> {}",
                event.getMemberEmail(), event.getPreviousBusinessId(), event.getNewBusinessId());

        try {
            Member member = memberService.findByEmail(event.getMemberEmail());

            switch (event.getChangeType()) {
                case BUSINESS_SWITCH:
                case BUSINESS_INFO_UPDATED:
                    Business newBusiness = businessRepository.findById(event.getNewBusinessId())
                            .orElse(null);
                    if (newBusiness != null) {
                        recommendationService.rebuildRecommendationsForBusiness(member, newBusiness);
                    }
                    break;

                case BUSINESS_DELETED:
                    recommendationService.clearRecommendationsForMember(member);
                    break;
            }

        } catch (Exception e) {
            log.error("대표 사업장 변경 이벤트 처리 실패: {}", event.getMemberEmail(), e);
        }
    }
}

package com.onmarket.notification.service.impl;

import com.onmarket.member.domain.Member;
import com.onmarket.member.exception.MemberNotFountException;
import com.onmarket.member.repository.MemberRepository;
import com.onmarket.notification.domain.NotificationHistory;
import com.onmarket.notification.domain.NotificationSubscription;
import com.onmarket.notification.domain.NotificationType;
import com.onmarket.notification.dto.NotificationListResponse;
import com.onmarket.notification.dto.NotificationSubscriptionRequest;
import com.onmarket.notification.dto.NotificationSummaryResponse;
import com.onmarket.notification.exception.NotificationNotFoundException;
import com.onmarket.notification.repository.NotificationHistoryRepository;
import com.onmarket.notification.repository.NotificationSubscriptionRepository;
import com.onmarket.notification.service.NotificationService;
import com.onmarket.notification.service.PushNotificationService;
import com.onmarket.post.domain.Post;
import com.onmarket.scrap.domain.Scrap;
import com.onmarket.scrap.repository.ScrapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationSubscriptionRepository subscriptionRepository;
    private final NotificationHistoryRepository historyRepository;
    private final MemberRepository memberRepository;
    private final ScrapRepository scrapRepository;
    private final PushNotificationService pushService; // 웹 푸시 발송 서비스

    @Override
    @Transactional
    public void subscribe(String email, NotificationSubscriptionRequest request) {
        Member member = findMemberByEmail(email);

        // 기존 구독이 있는지 확인
        Optional<NotificationSubscription> existingSubscription =
                subscriptionRepository.findByMemberMemberId(member.getMemberId());

        if (existingSubscription.isPresent()) {
            // 기존 구독 정보 업데이트
            existingSubscription.get().updateSubscription(
                    request.getEndpoint(),
                    request.getP256dhKey(),
                    request.getAuthKey()
            );
            log.info("사용자 {}의 구독 정보 업데이트", email);
        } else {
            // 새 구독 생성
            NotificationSubscription subscription = NotificationSubscription.create(
                    member,
                    request.getEndpoint(),
                    request.getP256dhKey(),
                    request.getAuthKey()
            );
            subscriptionRepository.save(subscription);
            log.info("사용자 {} 새 구독 생성", email);
        }
    }

    @Override
    @Transactional
    public void unsubscribe(String email) {
        Member member = findMemberByEmail(email);

        subscriptionRepository.findByMemberMemberId(member.getMemberId())
                .ifPresent(subscription -> {
                    subscription.deactivate();
                    log.info("사용자 {} 구독 비활성화", email);
                });
    }

    @Override
    public boolean isSubscribed(String email) {
        Member member = findMemberByEmail(email);
        return subscriptionRepository.findByMemberMemberId(member.getMemberId())
                .map(NotificationSubscription::getIsActive)
                .orElse(false);
    }

    @Override
    public Page<NotificationListResponse> getNotifications(String email, Pageable pageable) {
        Member member = findMemberByEmail(email);

        Page<NotificationHistory> notifications =
                historyRepository.findByMemberOrderByCreatedAtDesc(member, pageable);

        return notifications.map(this::convertToListResponse);
    }

    @Override
    public NotificationSummaryResponse getNotificationSummary(String email) {
        Member member = findMemberByEmail(email);

        Long unreadCount = historyRepository.countByMemberAndIsReadFalse(member);
        boolean hasSubscription = subscriptionRepository.findByMemberMemberId(member.getMemberId())
                .map(NotificationSubscription::getIsActive)
                .orElse(false);

        return NotificationSummaryResponse.of(unreadCount, hasSubscription);
    }

    @Override
    @Transactional
    public int markAllAsRead(String email) {
        Member member = findMemberByEmail(email);

        int updatedCount = historyRepository.markAllAsReadByMember(member);
        log.info("사용자 {}의 {} 개 알림을 읽음 처리", email, updatedCount);

        return updatedCount;
    }

    @Override
    @Transactional
    public void markAsRead(String email, Long notificationId) {
        Member member = findMemberByEmail(email);

        NotificationHistory notification = historyRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException());

        // 본인의 알림인지 확인
        if (!notification.getMember().equals(member)) {
            throw new IllegalArgumentException("본인의 알림만 읽음 처리할 수 있습니다.");
        }

        notification.markAsRead();
        log.info("알림 {} 읽음 처리", notificationId);
    }

    @Override
    @Transactional
    public void createDeadlineNotifications() {
        log.info("마감일 알림 생성 작업 시작");

        // 모든 스크랩 정보 조회 (N+1 방지)
        List<Scrap> scraps = scrapRepository.findAllWithMemberAndPost();

        for (Scrap scrap : scraps) {
            try {
                processDeadlineNotification(scrap);
            } catch (Exception e) {
                log.error("스크랩 {} 알림 처리 중 오류: {}", scrap.getScrapId(), e.getMessage());
            }
        }

        log.info("마감일 알림 생성 작업 완료");
    }

    // === Private 헬퍼 메서드들 ===

    private void processDeadlineNotification(Scrap scrap) {
        Member member = scrap.getMember();
        Post post = scrap.getPost();

        // 마감일이 없으면 스킵
        String deadlineStr = post.getDeadline();
        if (deadlineStr == null || deadlineStr.trim().isEmpty()) {
            return;
        }

        try {
            LocalDate deadline = LocalDate.parse(deadlineStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
            long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), deadline);

            // D-3, D-1, D-DAY인지 확인
            NotificationType notificationType = NotificationType.fromDDay(daysRemaining);
            if (notificationType == null) {
                return; // 알림 대상이 아님
            }

            // 오늘 이미 같은 타입 알림을 보냈는지 확인
            if (historyRepository.findTodayNotification(member, post, notificationType).isPresent()) {
                return; // 중복 알림 방지
            }

            // 알림 히스토리 생성
            String title = notificationType.createTitle(post.getProductName());
            String message = notificationType.createMessage(post.getProductName());

            NotificationHistory history = NotificationHistory.create(
                    member, post, notificationType, title, message
            );
            historyRepository.save(history);

            // 사용자가 구독 중이면 실제 푸시 발송
            Optional<NotificationSubscription> subscription =
                    subscriptionRepository.findByMemberMemberId(member.getMemberId());

            if (subscription.isPresent() && subscription.get().getIsActive()) {
                try {
                    pushService.sendPushNotification(subscription.get(), title, message);
                    history.markAsSent();
                    log.info("사용자 {}에게 {} 알림 발송 성공", member.getEmail(), notificationType);
                } catch (Exception e) {
                    history.markAsFailed(e.getMessage());
                    log.error("사용자 {}에게 푸시 발송 실패: {}", member.getEmail(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.warn("마감일 파싱 실패: {} - {}", deadlineStr, e.getMessage());
        }
    }

    private Member findMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberNotFountException());
    }

    private NotificationListResponse convertToListResponse(NotificationHistory notification) {
        return NotificationListResponse.builder()
                .notificationId(notification.getNotificationId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .notificationType(notification.getNotificationType())
                .postId(notification.getPost().getPostId())
                .productName(notification.getPost().getProductName())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}

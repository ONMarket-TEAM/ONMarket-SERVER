package com.onmarket.notification.domain;

import com.onmarket.common.domain.BaseTimeEntity;
import com.onmarket.member.domain.Member;
import com.onmarket.post.domain.Post;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_history",
        indexes = {
                @Index(name = "idx_member_created_at", columnList = "member_id, created_at DESC"),
                @Index(name = "idx_member_is_read", columnList = "member_id, is_read")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NotificationHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    // Member와 연결 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // Post와 연결 (N:1) - 어떤 정책에 대한 알림인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // 알림 타입
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    // 알림 제목
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    // 알림 내용
    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    // 읽음 여부
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    // 푸시 전송 성공 여부
    @Column(name = "is_sent", nullable = false)
    @Builder.Default
    private Boolean isSent = false;

    // 알림 생성 메서드
    public static NotificationHistory create(Member member, Post post, NotificationType type,
                                             String title, String message) {
        return NotificationHistory.builder()
                .member(member)
                .post(post)
                .notificationType(type)
                .title(title)
                .message(message)
                .isRead(false)
                .isSent(false)
                .build();
    }

    // 읽음 처리
    public void markAsRead() {
        this.isRead = true;
    }

    // 전송 성공 처리
    public void markAsSent() {
        this.isSent = true;
    }

    // 전송 실패 처리
    public void markAsFailed(String reason) {
        this.isSent = false;
    }
}

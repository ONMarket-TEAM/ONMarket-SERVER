package com.onmarket.notification.domain;

import com.onmarket.common.domain.BaseTimeEntity;
import com.onmarket.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_subscription",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"member_id"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NotificationSubscription extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subscriptionId;

    // Member와 연결 (1:1 관계)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // Web Push 구독 정보 (JSON으로 저장)
    @Column(name = "endpoint", nullable = false, length = 1000)
    private String endpoint;

    @Column(name = "p256dh_key", nullable = false, length = 500)
    private String p256dhKey;

    @Column(name = "auth_key", nullable = false, length = 500)
    private String authKey;

    // 구독 활성화 여부
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // 구독 정보 생성 메서드
    public static NotificationSubscription create(Member member, String endpoint, String p256dhKey, String authKey) {
        return NotificationSubscription.builder()
                .member(member)
                .endpoint(endpoint)
                .p256dhKey(p256dhKey)
                .authKey(authKey)
                .isActive(true)
                .build();
    }

    // 구독 비활성화
    public void deactivate() {
        this.isActive = false;
    }

    // 구독 정보 업데이트
    public void updateSubscription(String endpoint, String p256dhKey, String authKey) {
        this.endpoint = endpoint;
        this.p256dhKey = p256dhKey;
        this.authKey = authKey;
        this.isActive = true;
    }
}

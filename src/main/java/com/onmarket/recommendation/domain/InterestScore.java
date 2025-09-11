package com.onmarket.recommendation.domain;

import com.onmarket.business.domain.Business;
import com.onmarket.common.domain.BaseTimeEntity;
import com.onmarket.member.domain.Member;
import com.onmarket.post.domain.Post;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "interest_score")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class InterestScore extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id")
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    private Double totalScore; // 총 관심도 스코어
    private Double viewScore; // 조회 기반 스코어
    private Double engagementScore; // 참여 기반 스코어 (스크랩, 평점)
    private Double timeScore; // 체류시간 기반 스코어

    private LocalDateTime lastCalculatedAt; // 마지막 계산 시간

    public void updateScore(Double viewScore, Double engagementScore, Double timeScore) {
        this.viewScore = viewScore;
        this.engagementScore = engagementScore;
        this.timeScore = timeScore;
        this.totalScore = (viewScore * 0.3) + (engagementScore * 0.5) + (timeScore * 0.2);
        this.lastCalculatedAt = LocalDateTime.now();
    }

    /**
     * 총합 스코어만 직접 업데이트 (상대적 조정용)
     */
    public void updateTotalScore(Double totalScore) {
        this.totalScore = totalScore;
        this.lastCalculatedAt = LocalDateTime.now();
    }
}

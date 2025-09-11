package com.onmarket.recommendation.domain;

import com.onmarket.common.domain.BaseTimeEntity;
import com.onmarket.member.domain.Member;
import com.onmarket.post.domain.Post;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_interaction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserInteraction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Enumerated(EnumType.STRING)
    private InteractionType interactionType; // VIEW, SCROLL, CLICK_LINK, SCRAP, UNSCRAP, RATING

    private Integer durationSeconds; // 체류시간 (초)
    private Integer scrollPercentage; // 스크롤 비율 (0-100)
    private Integer rating; // 평점 (1-5)
    private String additionalData; // 추가 데이터 (JSON 형태)
}
package com.onmarket.post.domain;

import com.onmarket.comment.domain.Comment;
import com.onmarket.common.domain.BaseTimeEntity;
import com.onmarket.recommendation.domain.UserInteraction;
import com.onmarket.scrap.domain.Scrap;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostType postType; // LOAN, SUPPORT

    // === 게시물 목록에서 보여질 기본 정보 ===
    @Column(nullable = false)
    private String productName; // 상품명

    @Column(columnDefinition = "TEXT")
    @Setter
    private String summary; // 요약 정보

    private String deadline; // 마감일

    private String companyName; // 금융회사명/제공기관명

    // === 상세 페이지에서 보여질 정보 ===
    private String joinLink; // 가입하기 링크

    // 나중에 다른 사람이 채워줄 필드들
    @Setter
    private String imageUrl; // 상품 이미지 URL

    @Column(columnDefinition = "TEXT")
    @Setter
    private String detailContent; // 상세 내용

    @Column(nullable = false)
    private int scrapCount = 0;

    // === 원본 데이터 추적용 ===
    @Column(nullable = false)
    private String sourceTable; // "CreditLoanProduct", "LoanProduct", "SupportProduct"

    @Column(nullable = false)
    private Long sourceId; // 원본 테이블의 ID

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Scrap> scraps = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserInteraction> interactions = new ArrayList<>();

    // 스크랩 수를 1 증가시키는 편의 메서드
    public void increaseScrapCount() {
        this.scrapCount++;
    }

    // 스크랩 수를 1 감소시키는 편의 메서드 (0 이하로 내려가지 않도록 방지)
    public void decreaseScrapCount() {
        this.scrapCount = Math.max(0, this.scrapCount - 1);
    }
}
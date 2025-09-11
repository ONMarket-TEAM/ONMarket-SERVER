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
    private PostType postType;

    @Column(nullable = false)
    @Setter
    private String productName;

    @Column(columnDefinition = "TEXT")
    @Setter
    private String summary;

    @Setter
    private String deadline;

    @Setter
    private String companyName;

    @Setter
    private String joinLink;

    @Setter
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    @Setter
    private String detailContent;

    @Column(nullable = false)
    private int scrapCount = 0;

    @Column(nullable = false)
    private String sourceTable;

    @Column(nullable = false)
    private Long sourceId;

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

    public void decreaseScrapCount() {
        this.scrapCount = Math.max(0, this.scrapCount - 1);
    }
}
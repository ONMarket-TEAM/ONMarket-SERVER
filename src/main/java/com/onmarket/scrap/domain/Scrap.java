package com.onmarket.scrap.domain;

import com.onmarket.common.domain.BaseTimeEntity;
import com.onmarket.member.domain.Member;
import com.onmarket.post.domain.Post;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "scrap",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"member_id", "post_id"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Scrap extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scrapId;

    // Member와 연결 (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // Post와 연결 (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // 스크랩 생성 메서드
    public static Scrap create(Member member, Post post) {
        return Scrap.builder()
                .member(member)
                .post(post)
                .build();
    }
}
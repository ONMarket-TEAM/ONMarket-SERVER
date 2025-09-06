package com.onmarket.post.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PostDetailWithScrapResponse {
    private Long postId;
    private String productName;
    private String imageUrl;
    private LocalDateTime createdAt;
    private String joinLink;
    private String detailContent;
    private boolean isScraped;    // 현재 사용자가 스크랩했는지
    private Long scrapCount;      // 전체 스크랩 개수

    // PostDetailResponse에서 변환하는 정적 팩토리 메서드
    public static PostDetailWithScrapResponse from(PostDetailResponse postDetail,
                                                   boolean isScraped,
                                                   Long scrapCount) {
        return PostDetailWithScrapResponse.builder()
                .postId(postDetail.getPostId())
                .productName(postDetail.getProductName())
                .imageUrl(postDetail.getImageUrl())
                .createdAt(postDetail.getCreatedAt())
                .joinLink(postDetail.getJoinLink())
                .detailContent(postDetail.getDetailContent())
                .isScraped(isScraped)
                .scrapCount(scrapCount)
                .build();
    }
}

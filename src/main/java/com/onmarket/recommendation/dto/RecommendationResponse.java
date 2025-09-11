package com.onmarket.recommendation.dto;

import com.onmarket.post.domain.PostType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class RecommendationResponse {
    private Long postId;
    private String productName;
    private String companyName;
    private PostType postType;
    private String deadline;
    private String summary;
    private String imageUrl;
    private Double interestScore;
    private String recommendationReason; // 추천 이유
}

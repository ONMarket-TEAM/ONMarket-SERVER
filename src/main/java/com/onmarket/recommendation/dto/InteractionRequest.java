package com.onmarket.recommendation.dto;

import com.onmarket.recommendation.domain.InteractionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class InteractionRequest {
    private Long postId;
    private InteractionType interactionType;
    private Integer durationSeconds;
    private Integer scrollPercentage;
    private Integer rating;
}
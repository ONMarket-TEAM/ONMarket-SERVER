package com.onmarket.scrap.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ScrapToggleResponse {
    private boolean isScraped;
    private Long scrapCount;
    private String message;

    public static ScrapToggleResponse of(boolean isScraped, Long scrapCount) {
        return ScrapToggleResponse.builder()
                .isScraped(isScraped)
                .scrapCount(scrapCount)
                .message(isScraped ? "스크랩했습니다️." : "스크랩 해제했습니다.")
                .build();
    }
}
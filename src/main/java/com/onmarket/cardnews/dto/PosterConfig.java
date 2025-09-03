package com.onmarket.cardnews.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PosterConfig {
    private String title;
    private String subtitle;
    private String badge;
    private List<Section> sections;
    private Theme theme;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Section {
        private String heading;
        private List<String> bullets; // bullet용
        private String text;          // text용
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Theme {
        private String accent;
        private String brand;
    }
}
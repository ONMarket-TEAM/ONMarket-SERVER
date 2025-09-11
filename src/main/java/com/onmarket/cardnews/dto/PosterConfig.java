package com.onmarket.cardnews.dto;

import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class PosterConfig {
    private String title;       // 포스터 메인 타이틀
    private String subtitle;    // 부제(선택)
    private String date;        // YYYY.MM.DD 또는 빈 값
    private String badge;       // "대출상품","정부지원금" 등
    private Theme theme;        // 색상 힌트
    private Operator operator;  // 주관기관
    private List<Section> sections; // 3개 섹션

    /** ✅ 신청 기간 카드 전용 라인들 (1번째 카드에 사용) */
    private List<String> applyPeriodLines = new ArrayList<>();

    @Getter @Setter @NoArgsConstructor
    public static class Theme {
        private String accent;  // 예: #0ea5b7
        private String brand;   // 예: #2563eb
    }

    @Getter @Setter @NoArgsConstructor
    public static class Operator {
        private String name;    // 부산은행, 보건복지부 등
        private String type;    // 은행|정부|지자체|공공기관|기업
    }

    @Getter @Setter @NoArgsConstructor
    public static class Section {
        private String heading;       // 섹션 제목
        private List<String> bullets; // 포인트(1~3)
        private String text;          // 선택 설명
    }
}

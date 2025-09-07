package com.onmarket.cardnews.service;

import lombok.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BrandPaletteResolver {

    @Getter @AllArgsConstructor
    public static class BrandPalette {
        private final String brand;   // "#RRGGBB"
        private final String accent;  // "#RRGGBB"
        private final boolean strict; // true면 프롬프트에서 강하게 강제
    }

    // 1) 초기 매핑(샘플) — 실제 브랜드 컬러는 운영사 가이드에 맞게 업데이트하세요.
    private static final Map<String, BrandPalette> MAP = new HashMap<>();
    static {
        // 공공/유관
        MAP.put("서민금융진흥원", new BrandPalette("#0FA9AB", "#F59E0B", true)); // TODO: 실제 가이드에 맞게 교체
        MAP.put("소상공인시장진흥공단", new BrandPalette("#2563EB", "#10B981", false));
        MAP.put("신용보증기금", new BrandPalette("#1C8B3D", "#0EA5A5", true));
        MAP.put("중소벤처기업부", new BrandPalette("#0EA5E9", "#F97316", false));
        MAP.put("한국전력공사", new BrandPalette("#E11D48", "#0EA5E9", false));
        // 은행
        MAP.put("IBK기업은행", new BrandPalette("#0067AC", "#00AEEF", true));
        MAP.put("국민은행", new BrandPalette("#FFC20E", "#3A3A3A", true));
        MAP.put("신한은행", new BrandPalette("#0E5AA7", "#7CB4E0", true));
        MAP.put("우리은행", new BrandPalette("#0067AC", "#00AEEF", false));
        MAP.put("하나은행", new BrandPalette("#00857C", "#38BDF8", false));
        MAP.put("농협은행주식회사", new BrandPalette("#0078C1", "#FFD400", false));
        MAP.put("주식회사 카카오뱅크", new BrandPalette("#FEE500", "#111111", true));
        MAP.put("주식회사 케이뱅크", new BrandPalette("#E4007F", "#111111", false));
        MAP.put("토스뱅크 주식회사", new BrandPalette("#1B64DA", "#60A5FA", false));
        MAP.put("한국산업은행", new BrandPalette("#005EB8", "#00A6D6", false));
        MAP.put("한국스탠다드차타드은행", new BrandPalette("#00A885", "#004D3F", false));
        // 신용보증재단(지역별은 동일 팔레트로 시작)
        Arrays.asList("서울신용보증재단","경기신용보증재단","울산신용보증재단","전북신용보증재단","충북신용보증재단",
                        "전남신용보증재단","광주신용보증재단","인천신용보증재단","강원신용보증재단","부산신용보증재단",
                        "경북신용보증재단","충남신용보증재단","제주신용보증재단","대구신용보증재단","전북특별자치도신용보증재단")
                .forEach(n -> MAP.put(n, new BrandPalette("#16A34A", "#10B981", false)));
        // TODO: 목록 계속 보강
    }
// BrandPaletteResolver.java

    // 1) 정규화: '은행'은 제거하지 말 것
    private String normalize(String s) {
        String n = s.replaceAll("[()\\s]", "");
        n = n.replaceAll("^(재단법인|재단|주식회사|\\(재\\)|\\(주\\))", "");
        // ▼ '은행'을 제거하던 부분에서 은행을 제외합니다.
        n = n.replaceAll("(공사|공단|재단|협회|진흥원|주식회사|체신|관리공단)$", "");
        return n;
    }

    // 2) 정규화된 맵을 만들어 정확/부분 일치 모두 정규화 기준으로 비교
    private static final Map<String, BrandPalette> NORM_MAP = new HashMap<>();
    static {
        // ... MAP.put(...) 들은 그대로 두고,
        MAP.forEach((k, v) -> NORM_MAP.put(k.replaceAll("[()\\s]", "")
                .replaceAll("^(재단법인|재단|주식회사|\\(재\\)|\\(주\\))", "")
                .replaceAll("(공사|공단|재단|협회|진흥원|주식회사|체신|관리공단)$", ""), v));
    }

    public Optional<BrandPalette> resolve(String rawName) {
        if (rawName == null || rawName.isBlank()) return Optional.empty();
        String key = normalize(rawName);

        // (a) 정규화 키로 정확 매칭
        BrandPalette exact = NORM_MAP.get(key);
        if (exact != null) return Optional.of(exact);

        // (b) 정규화 키로 부분/유사 매칭 (방향 양쪽)
        for (var e : NORM_MAP.entrySet()) {
            String nk = e.getKey();
            if (key.contains(nk) || nk.contains(key)) {
                return Optional.of(e.getValue());
            }
        }
        return Optional.empty();
    }
}
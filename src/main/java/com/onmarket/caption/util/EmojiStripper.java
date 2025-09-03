// com/onmarket/caption/util/EmojiStripper.java
package com.onmarket.caption.util;

public class EmojiStripper {
    // 광범위 이모지 제거(백업 안전장치)
    public static String strip(String input) {
        if (input == null) return null;
        // 기본 BMP 바깥(서러게이트) 및 공통 이모지 범주 제거
        return input
                .replaceAll("[\\p{So}\\x{1F300}-\\x{1FAD6}\\x{1F900}-\\x{1F9FF}\\x{2600}-\\x{27BF}]", "")
                .replaceAll("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]", "");
    }
}
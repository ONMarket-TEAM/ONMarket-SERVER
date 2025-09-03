package com.onmarket.supportsdata.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class SimpleDateParser {

    // 날짜 추출 정규표현식들
    private static final Pattern FULL_DATE_RANGE = Pattern.compile(
            "(\\d{4})[.-](\\d{1,2})[.-](\\d{1,2})\\s*[~∼-]\\s*(\\d{4})[.-](\\d{1,2})[.-](\\d{1,2})"
    );

    private static final Pattern SHORT_DATE_RANGE = Pattern.compile(
            "'?(\\d{2})[.-](\\d{1,2})[.-](\\d{1,2})[.]*\\s*[~∼-]\\s*'?(\\d{1,2})[.-](\\d{1,2})[.]?"
    );

    // 다중 차수 패턴 (1차, 2차 등)
    private static final Pattern MULTI_PHASE_PATTERN = Pattern.compile(
            "\\((\\d+)차\\)[^\\(]*?'?(\\d{2})[.-](\\d{1,2})[.-](\\d{1,2})[.]*\\s*[~∼-]\\s*'?(\\d{1,2})[.-](\\d{1,2})[.]?"
    );

    private static final Pattern SINGLE_FULL_DATE = Pattern.compile(
            "(\\d{4})[.-](\\d{1,2})[.-](\\d{1,2})"
    );

    private static final Pattern MONTH_RANGE = Pattern.compile(
            "(\\d{1,2})월\\s*[~∼-]\\s*(\\d{1,2})월"
    );

    private static final Pattern SINGLE_MONTH = Pattern.compile("(\\d{1,2})월");

    public DatePair parseApplicationDeadline(String deadlineText) {
        if (deadlineText == null || deadlineText.trim().isEmpty()) {
            return new DatePair(null, null);
        }

        String text = deadlineText.trim();

        // 상시모집 키워드 확인
        if (isAlwaysAvailable(text)) {
            return new DatePair(null, null);
        }

        // 0. 먼저 다중 차수 패턴 확인 (1차, 2차 등)
        List<PhaseInfo> phases = parseMultiplePhases(text);
        if (!phases.isEmpty()) {
            PhaseInfo closestPhase = getClosestValidPhase(phases);
            if (closestPhase != null) {
                return new DatePair(closestPhase.startDate, closestPhase.endDate);
            }
        }

        // 1. 완전한 날짜 범위 파싱 (2025.01.01~2025.12.31)
        Matcher fullRangeMatcher = FULL_DATE_RANGE.matcher(text);
        if (fullRangeMatcher.find()) {
            String startDate = formatDate(
                    fullRangeMatcher.group(1), // year
                    fullRangeMatcher.group(2), // month
                    fullRangeMatcher.group(3)  // day
            );
            String endDate = formatDate(
                    fullRangeMatcher.group(4), // year
                    fullRangeMatcher.group(5), // month
                    fullRangeMatcher.group(6)  // day
            );
            return new DatePair(startDate, endDate);
        }

        // 2. 짧은 날짜 범위 파싱 ('25.2.24~3.7)
        Matcher shortRangeMatcher = SHORT_DATE_RANGE.matcher(text);
        if (shortRangeMatcher.find()) {
            String year = "20" + shortRangeMatcher.group(1);
            String startDate = formatDate(year, shortRangeMatcher.group(2), shortRangeMatcher.group(3));
            String endDate = formatDate(year, shortRangeMatcher.group(4), shortRangeMatcher.group(5));
            return new DatePair(startDate, endDate);
        }

        // 3. 단일 완전한 날짜 (2025.03.10)
        Matcher singleDateMatcher = SINGLE_FULL_DATE.matcher(text);
        if (singleDateMatcher.find()) {
            String date = formatDate(
                    singleDateMatcher.group(1), // year
                    singleDateMatcher.group(2), // month
                    singleDateMatcher.group(3)  // day
            );
            return new DatePair(null, date); // 마감일만 설정
        }

        // 4. 월 범위 (3월~4월)
        Matcher monthRangeMatcher = MONTH_RANGE.matcher(text);
        if (monthRangeMatcher.find()) {
            String currentYear = String.valueOf(java.time.Year.now().getValue());
            String startDate = formatDate(currentYear, monthRangeMatcher.group(1), "01");
            String endMonth = monthRangeMatcher.group(2);
            String endDate = formatDate(currentYear, endMonth, getLastDayOfMonth(currentYear, endMonth));
            return new DatePair(startDate, endDate);
        }

        // 5. 단일 월 (3월)
        Matcher singleMonthMatcher = SINGLE_MONTH.matcher(text);
        if (singleMonthMatcher.find()) {
            String currentYear = String.valueOf(java.time.Year.now().getValue());
            String month = singleMonthMatcher.group(1);
            String startDate = formatDate(currentYear, month, "01");
            String endDate = formatDate(currentYear, month, getLastDayOfMonth(currentYear, month));
            return new DatePair(startDate, endDate);
        }

        // 파싱 불가능한 경우 null 반환 (상시모집으로 처리)
        return new DatePair(null, null);
    }

    // 다중 차수 파싱
    private List<PhaseInfo> parseMultiplePhases(String text) {
        List<PhaseInfo> phases = new ArrayList<>();
        Matcher matcher = MULTI_PHASE_PATTERN.matcher(text);

        while (matcher.find()) {
            try {
                int phaseNum = Integer.parseInt(matcher.group(1)); // 차수
                String year = "20" + matcher.group(2); // 년도
                String startMonth = matcher.group(3);
                String startDay = matcher.group(4);
                String endMonth = matcher.group(5);
                String endDay = matcher.group(6);

                String startDate = formatDate(year, startMonth, startDay);
                String endDate = formatDate(year, endMonth, endDay);

                if (startDate != null && endDate != null) {
                    phases.add(new PhaseInfo(phaseNum, startDate, endDate));
                }
            } catch (Exception e) {
                log.debug("Failed to parse phase: {}", matcher.group());
            }
        }

        return phases;
    }

    // 현재 날짜 기준으로 아직 지나지 않은 가장 가까운 차수 선택
    private PhaseInfo getClosestValidPhase(List<PhaseInfo> phases) {
        String today = java.time.LocalDate.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
        );

        PhaseInfo closestPhase = null;

        for (PhaseInfo phase : phases) {
            // 마감일이 오늘보다 이후인 것만 고려
            if (phase.endDate.compareTo(today) >= 0) {
                if (closestPhase == null ||
                        phase.startDate.compareTo(closestPhase.startDate) < 0) {
                    closestPhase = phase;
                }
            }
        }

        return closestPhase;
    }

    private boolean isAlwaysAvailable(String text) {
        return text.contains("상시") ||
                text.contains("연중") ||
                text.contains("별도") ||
                text.contains("문의") ||
                text.contains("공고") ||
                text.contains("상이") ||
                text.equals("신청없음");
    }

    private String formatDate(String year, String month, String day) {
        try {
            int y = Integer.parseInt(year);
            int m = Integer.parseInt(month);
            int d = Integer.parseInt(day);

            return String.format("%04d%02d%02d", y, m, d);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getLastDayOfMonth(String year, String month) {
        try {
            java.time.YearMonth yearMonth = java.time.YearMonth.of(
                    Integer.parseInt(year),
                    Integer.parseInt(month)
            );
            return String.valueOf(yearMonth.lengthOfMonth());
        } catch (Exception e) {
            return "31"; // 기본값
        }
    }

    // 차수 정보를 담는 내부 클래스
    private static class PhaseInfo {
        final int phaseNum;
        final String startDate;
        final String endDate;

        PhaseInfo(int phaseNum, String startDate, String endDate) {
            this.phaseNum = phaseNum;
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }

    // 결과를 담는 간단한 클래스
    public static class DatePair {
        public final String startDay;
        public final String endDay;

        public DatePair(String startDay, String endDay) {
            this.startDay = startDay;
            this.endDay = endDay;
        }
    }
}
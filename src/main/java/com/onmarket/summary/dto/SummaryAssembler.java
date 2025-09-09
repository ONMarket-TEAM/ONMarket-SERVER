package com.onmarket.summary.dto;

import com.onmarket.loandata.domain.LoanProduct;
import com.onmarket.fssdata.domain.CreditLoanProduct;
import com.onmarket.fssdata.domain.CreditLoanOption;
import com.onmarket.supportsdata.domain.SupportProduct;

import java.text.NumberFormat;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.stream.DoubleStream;

public class SummaryAssembler {

    private static final NumberFormat WON = NumberFormat.getInstance(Locale.KOREA);

    /** loan_product용 텍스트 */
    public static String forLoan(LoanProduct p) {
        // loanLimit는 문자열(“1000”, “1,000”, “최대 1000” 등) → 숫자(원)로 안전 변환
        String limit = "-";
        Long limitWon = parseManwonToWon(p.getLoanLimit()); // “1000”(만원) -> 10,000,000(원)
        if (limitWon != null && limitWon > 0) {
            limit = WON.format(limitWon) + "원";
        }

        String term  = nz(p.getMaxTotalTerm()) != null ? p.getMaxTotalTerm() + "개월" : "-";

        return """
               [상품명] %s
               [용도] %s
               [대상] %s
               [기관] %s
               [금리구분] %s
               [금리] %s
               [총한도] %s
               [최대기간] %s
               [특별조건] %s
               """.formatted(
                nz(p.getProductName()),
                nz(p.getUsage()),
                nz(p.getTarget()),
                nz(p.getOfferingInstitution()),
                nz(p.getInterestCategory()),
                nz(p.getInterestRate()),
                limit,
                term,
                nz(p.getSpecialTargetConditions())
        );
    }

    /** credit_loan_product용 텍스트 */
    public static String forCredit(CreditLoanProduct p, List<CreditLoanOption> opts) {
        String inst = "%s(%s)".formatted(nz(p.getKorCoNm()), nz(p.getFinCoNo()));
        String rateLine = "-";

        if (opts != null && !opts.isEmpty()) {
            // 평균금리 필드가 있다면 사용 (없으면 주석 처리하거나 다른 필드로 보완)
            DoubleStream ds = opts.stream()
                    .map(CreditLoanOption::getCrdtGradAvg) // 엔티티에 없으면 이 줄을 주석 처리하세요.
                    .filter(v -> v != null && v > 0)
                    .mapToDouble(Double::doubleValue);

            DoubleSummaryStatistics st = ds.summaryStatistics();
            if (st.getCount() > 0) {
                rateLine = String.format("평균금리 약 %.2f%% ~ %.2f%%", st.getMin(), st.getMax());
            }
        }

        return """
               [상품명] %s
               [기관] %s
               [유형] %s (%s)
               [가입방법] %s
               [공시기간] %s ~ %s
               [금리요약] %s
               """.formatted(
                nz(p.getFinPrdtNm()),
                inst,
                nz(p.getCrdtPrdtTypeNm()),
                nz(p.getCrdtPrdtType()),
                nz(p.getJoinWay()),
                nz(p.getDclsStrtDay()),
                nz(p.getDclsEndDay()),
                rateLine
        );
    }

    // ---------- helpers ----------
    private static String nz(Object x) {
        if (x == null) return "-";
        String s = String.valueOf(x).trim();
        return (s.isEmpty() || "null".equalsIgnoreCase(s)) ? "-" : s;
    }

    /** "1000", "1,000", "최대 1000" (만원 단위 가정) → 원(Long) */
    private static Long parseManwonToWon(String manwonStr) {
        if (manwonStr == null) return null;
        // 숫자와 점만 남김 (소수점 있으면 처리)
        String cleaned = manwonStr.replaceAll("[^0-9.]", "");
        if (cleaned.isEmpty()) return null;
        try {
            double manwon = Double.parseDouble(cleaned); // 예: 1000.0 (만원)
            double won = manwon * 10_000d;
            return (long) won;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    public static String forSupport(SupportProduct s) {
        return """
           [서비스명] %s
           [지원유형] %s
           [목적요약] %s
           [지원대상] %s
           [선정기준] %s
           [지원내용] %s
           [신청방법] %s
           [담당부서/문의] %s / %s
           [신청마감] %s
           """.formatted(
                nz(s.getServiceName()),
                nz(s.getSupportType()),
                nz(s.getServicePurposeSummary()),
                nz(s.getSupportTarget()),
                nz(s.getSelectionCriteria()),
                nz(s.getSupportContent()),
                nz(s.getApplicationMethod()),
                nz(s.getDepartmentName()),
                nz(s.getContact()),
                nz(s.getApplicationDeadline())
        );
    }
}
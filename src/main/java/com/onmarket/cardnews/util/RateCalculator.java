package com.onmarket.cardnews.util;

import com.onmarket.cardnews.dto.CreditLoanOptionDto;
import java.util.*;

public class RateCalculator {
    /**
     * Compute final grade rates for a given product from options list.
     * Formula: final = 기준금리(B) + 가산금리(C) + 가감조정금리(D). If 대출금리(A) present, prefer A.
     */
    public static Map<String, Double> computeFinalRates(List<CreditLoanOptionDto> options) {
        Map<String, Double> res = new HashMap<>();
        Map<String, Double> A = gradeMap(findType(options, "A")); // 대출금리
        Map<String, Double> B = gradeMap(findType(options, "B")); // 기준금리
        Map<String, Double> C = gradeMap(findType(options, "C")); // 가산금리
        Map<String, Double> D = gradeMap(findType(options, "D")); // 가감조정금리

// grades to consider
        String[] grades = {"1","4","5","6","10","11","12","13","Avg"};
        for (String g : grades) {
            Double val = null;
            if (A.get(g) != null) {
                val = A.get(g);
            } else if (B.get(g) != null || C.get(g) != null || D.get(g) != null) {
                val = nz(B.get(g)) + nz(C.get(g)) + nz(D.get(g));
            }
            if (val != null) res.put(g, round1(val));
        }
        return res;
    }

    private static CreditLoanOptionDto findType(List<CreditLoanOptionDto> list, String type) {
        if (list == null) return null;
        return list.stream().filter(o -> type.equalsIgnoreCase(o.getCrdtLendRateType())).findFirst().orElse(null);
    }

    private static double nz(Double d) { return d == null ? 0.0 : d; }

    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }

    private static Map<String, Double> gradeMap(CreditLoanOptionDto o) {
        Map<String, Double> m = new HashMap<>();
        if (o == null) return m;
        if (o.getCrdtGrad1() != null) m.put("1", o.getCrdtGrad1());
        if (o.getCrdtGrad4() != null) m.put("4", o.getCrdtGrad4());
        if (o.getCrdtGrad5() != null) m.put("5", o.getCrdtGrad5());
        if (o.getCrdtGrad6() != null) m.put("6", o.getCrdtGrad6());
        if (o.getCrdtGrad10() != null) m.put("10", o.getCrdtGrad10());
        if (o.getCrdtGrad11() != null) m.put("11", o.getCrdtGrad11());
        if (o.getCrdtGrad12() != null) m.put("12", o.getCrdtGrad12());
        if (o.getCrdtGrad13() != null) m.put("13", o.getCrdtGrad13());
        if (o.getCrdtGradAvg() != null) m.put("Avg", o.getCrdtGradAvg());
        return m;
    }
}
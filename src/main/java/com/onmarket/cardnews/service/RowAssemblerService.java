package com.onmarket.cardnews.service;
import com.onmarket.cardnews.dto.CardNewsRequest;
import com.onmarket.cardnews.dto.CreditLoanProductDto;
import com.onmarket.cardnews.dto.LoanProductDto;
import com.onmarket.cardnews.dto.SupportServiceDto;
import com.onmarket.cardnews.util.RateCalculator;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RowAssemblerService {
    public String toRowText(CardNewsRequest req) {
        if (StringUtils.isNotBlank(req.getRowText())) return req.getRowText();

        StringBuilder sb = new StringBuilder();

        if (req.getLoanProduct() != null) {
            LoanProductDto p = req.getLoanProduct();
            sb.append("상품명: ").append(n(p.getProductName())).append("\n");
            sb.append("대출 대상: ").append(n(p.getTarget())).append(" (필터: ").append(n(p.getTargetFilter())).append(")\n");
            sb.append("대출 용도: ").append(n(p.getUsage())).append("\n");
            sb.append("금리 구분/금리: ").append(n(p.getInterestCategory())).append(" / ").append(n(p.getInterestRate())).append("%\n");
            sb.append("한도: ").append(n(p.getLoanLimit())).append("만원\n");
            sb.append("상환 방식: ").append(n(p.getRepaymentMethod())).append(" / 총 ")
                    .append(n(p.getMaxTotalTerm())).append("개월, 거치 ")
                    .append(n(p.getMaxDeferredTerm())).append("개월\n");
            sb.append("공급처: ").append(n(p.getOfferingInstitution())).append(" (분류: ")
                    .append(n(p.getInstitutionCategory())).append(")\n");
            if (StringUtils.isNotBlank(p.getIncome())) sb.append("소득 조건: ").append(p.getIncome()).append("\n");
            if (StringUtils.isNotBlank(p.getSpecialTargetConditions())) sb.append("특별 대상: ").append(p.getSpecialTargetConditions()).append("\n");
            if (StringUtils.isNotBlank(p.getOtherReference())) sb.append("기타: ").append(p.getOtherReference()).append("\n");
        }

        if (req.getCreditLoanProduct() != null) {
            CreditLoanProductDto p = req.getCreditLoanProduct();
            sb.append("신용대출 상품명: ").append(n(p.getFinPrdtNm())).append(" (회사: ").append(n(p.getKorCoNm())).append(")\n");
            sb.append("상품코드: ").append(n(p.getFinPrdtCd())).append(", 가입: ").append(n(p.getJoinWay())).append("\n");

            Map<String, Double> rates = RateCalculator.computeFinalRates(req.getCreditLoanOptions());
            if (!rates.isEmpty()) {
                sb.append("[예상 최종금리] 등급별: ");
                rates.forEach((g, v) -> sb.append(g).append("등급 ").append(v).append("% "));
                sb.append("\n");
            }
        }

        if (req.getSupportService() != null) {
            SupportServiceDto s = req.getSupportService();
            sb.append("서비스명: ").append(n(s.getServiceName())).append(" / 유형: ").append(n(s.getSupportType())).append("\n");
            sb.append("목적: ").append(n(s.getServicePurposeSummary())).append("\n");
            sb.append("지원대상: ").append(n(s.getSupportTarget())).append(" (사용자: ").append(n(s.getUserCategory())).append(")\n");
            sb.append("지원내용: ").append(n(s.getSupportContent())).append("\n");
            sb.append("신청방법: ").append(n(s.getApplicationMethod())).append(", 마감: ").append(n(s.getApplicationDeadline())).append("\n");
            sb.append("문의: ").append(n(s.getContact())).append(" / URL: ").append(n(s.getOnlineApplicationUrl())).append("\n");
            if (req.getSupportConditions() != null && !req.getSupportConditions().isEmpty()) {
                sb.append("조건: ");
                req.getSupportConditions().stream().limit(1).forEach(c -> {
                    sb.append("연령").append("(").append(c.getAgeStart()).append("~").append(c.getAgeEnd()).append(") ");
                    if ("Y".equalsIgnoreCase(c.getBusinessOperating())) sb.append("운영기업 ");
                    if ("Y".equalsIgnoreCase(c.getBusinessProspective())) sb.append("창업기업 ");
                    if ("Y".equalsIgnoreCase(c.getHouseholdNoHome())) sb.append("무주택 ");
                });
                sb.append("\n");
            }
        }

        String out = sb.toString().trim();
        if (out.isEmpty()) out = "대출/지원 상품 안내 텍스트가 비어있습니다.";
        return out;
    }

    private String n(Object o) { return o == null ? "-" : String.valueOf(o); }
}
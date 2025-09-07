package com.onmarket.cardnews.service;

import com.onmarket.cardnews.dto.TargetType;
import com.onmarket.fssdata.domain.CreditLoanOption;
import com.onmarket.fssdata.domain.CreditLoanProduct;
import com.onmarket.fssdata.repository.CreditLoanProductRepository;
import com.onmarket.loandata.domain.LoanProduct;
import com.onmarket.loandata.repository.LoanProductRepository;
import com.onmarket.supportsdata.domain.SupportCondition;
import com.onmarket.supportsdata.domain.SupportProduct;
import com.onmarket.supportsdata.repository.SupportProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RowAssemblerService {

    private final LoanProductRepository loanRepo;
    private final CreditLoanProductRepository creditRepo;
    private final SupportProductRepository supportRepo;

    public String assemble(TargetType type, String idRaw) {
        return switch (type) {
            case LOAN_PRODUCT        -> assembleLoanProduct(Long.parseLong(idRaw));
            case CREDIT_LOAN_PRODUCT -> assembleCreditLoanProduct(Long.parseLong(idRaw));
            case SUPPORT_PRODUCT     -> assembleSupportProduct(Long.parseLong(idRaw)); // ← String → Long
        };
    }

    private String assembleLoanProduct(Long id) {
        LoanProduct p = loanRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("LoanProduct not found: " + id));

        StringBuilder sb = new StringBuilder();
        sb.append("배지: 대출상품\n"); // ← 프롬프트 일관성(팔레트 선택) 위해 명시
        sb.append("상품명: ").append(n(p.getProductName())).append("\n");
        sb.append("용도: ").append(n(p.getUsage())).append("\n");
        sb.append("대상: ").append(n(p.getTarget())).append("\n");
        sb.append("제공기관: ").append(n(p.getOfferingInstitution())).append("\n");
        if (p.getInterestCategory()!=null || p.getInterestRate()!=null) {
            sb.append("금리: ").append(n(p.getInterestCategory())).append(" / ").append(n(p.getInterestRate())).append("\n");
        }
        if (p.getLoanLimit()!=null) sb.append("한도: ").append(n(p.getLoanLimit())).append("\n");
        if (p.getMaxTotalTerm()!=null) sb.append("기간: ").append(n(p.getMaxTotalTerm())).append("개월\n");
        if (p.getRepaymentMethod()!=null) sb.append("상환방식: ").append(n(p.getRepaymentMethod())).append("\n");
        if (p.getAge()!=null) sb.append("연령조건: ").append(p.getAge()).append("\n");
        if (p.getSpecialTargetConditions()!=null) sb.append("특별대상: ").append(p.getSpecialTargetConditions()).append("\n");
        if (p.getRelatedSite()!=null) sb.append("관련사이트: ").append(p.getRelatedSite()).append("\n");
        return sb.toString();
    }

    private String assembleCreditLoanProduct(Long id) {
        CreditLoanProduct cp = creditRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("CreditLoanProduct not found: " + id));

        StringBuilder sb = new StringBuilder();
        sb.append("배지: 대출상품\n"); // ← 일관성
        sb.append("상품명: ").append(n(cp.getFinPrdtNm())).append("\n");
        sb.append("기관명: ").append(n(cp.getKorCoNm())).append("\n");
        if (cp.getCrdtPrdtTypeNm()!=null) sb.append("상품종류: ").append(cp.getCrdtPrdtTypeNm()).append("\n");
        if (cp.getJoinWay()!=null) sb.append("가입방법: ").append(cp.getJoinWay()).append("\n");
        if (cp.getCbName()!=null) sb.append("신용평가기관: ").append(cp.getCbName()).append("\n");

        Optional<CreditLoanOption> avgOpt = cp.getOptions().stream()
                .filter(o -> o.getCrdtGradAvg()!=null)
                .findFirst();
        avgOpt.ifPresent(o -> sb.append("평균 금리: ").append(trimZero(o.getCrdtGradAvg())).append("%\n"));

        if (cp.getRltSite()!=null) sb.append("관련사이트: ").append(cp.getRltSite()).append("\n");
        return sb.toString();
    }

    private String assembleSupportProduct(Long id) { // ← String → Long
        SupportProduct sp = supportRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("SupportProduct not found: " + id));

        StringBuilder sb = new StringBuilder();
        sb.append("배지: 정부지원금\n"); // ← 일관성
        sb.append("정책명: ").append(n(sp.getServiceName())).append("\n");
        sb.append("유형: ").append(n(sp.getSupportType())).append("\n");
        if (sp.getServicePurposeSummary()!=null) sb.append("목적: ").append(sp.getServicePurposeSummary()).append("\n");
        if (sp.getSupportTarget()!=null) sb.append("지원대상: ").append(sp.getSupportTarget()).append("\n");
        if (sp.getSupportContent()!=null) sb.append("지원내용: ").append(sp.getSupportContent()).append("\n");
        if (sp.getApplicationMethod()!=null) sb.append("신청방법: ").append(sp.getApplicationMethod()).append("\n");
        sb.append("마감일: ").append(sp.getDisplayDeadline()).append("\n");
        if (sp.getContact()!=null) sb.append("문의처: ").append(sp.getContact()).append("\n");
        if (sp.getDetailUrl()!=null) sb.append("상세URL: ").append(sp.getDetailUrl()).append("\n");

        SupportCondition c = sp.getSupportCondition();
        if (c!=null) {
            if (c.getAgeStart()!=null || c.getAgeEnd()!=null) {
                sb.append("연령조건: ")
                        .append(c.getAgeStart()!=null?c.getAgeStart():"?")
                        .append("~")
                        .append(c.getAgeEnd()!=null?c.getAgeEnd():"?")
                        .append("세\n");
            }
            if ("Y".equals(c.getJobEmployee())) sb.append("재직자 가능\n");
            if ("Y".equals(c.getJobSeeker())) sb.append("구직자 가능\n");
            if ("Y".equals(c.getBusinessProspective())) sb.append("창업기업 대상\n");
            if ("Y".equals(c.getBusinessOperating())) sb.append("운영기업 대상\n");
        }
        return sb.toString();
    }

    private String n(String v){ return v==null ? "" : v.trim(); }
    private String trimZero(Double d){
        String s = String.valueOf(d);
        return s.endsWith(".0") ? s.substring(0,s.length()-2) : s;
    }
}
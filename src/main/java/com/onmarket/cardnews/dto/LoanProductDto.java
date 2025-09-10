package com.onmarket.cardnews.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanProductDto {
    private Long id; private Integer sequence; private String productName; private String usage;
    private String target; private String targetFilter; private String institutionCategory;
    private String offeringInstitution; private String guaranteeInstitution; private String handlingInstitution;
    private String handlingInstitutionDetailView; private String repaymentMethod; private String interestCategory;
    private String interestRate; private String loanLimit; private Integer maxTotalTerm; private Integer maxDeferredTerm;
    private Integer maxRepaymentTerm; private String age; private Integer ageBelow39; private Integer ageAbove40;
    private Integer ageAbove60; private String income; private String incomeCondition; private String specialTargetConditions;
    private String otherReference; private String repaymentFee; private String loanInsuranceCost;
    private String productCategory; private String productOperationPeriod; private Integer isKinfaProduct;
    private String kinfaProductEtc; private String relatedSite;
}
package com.onmarket.loandata.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "loan_product")
public class LoanProductEntity {

    @Id
    @Column(name = "product_seq")
    private String seq;

    private String productName;
    private String handlingInstitution;
    private String interestRateType;
    private String interestRate;
    private String loanLimit;
    private String maxLoanTerm;
    private String loanPurpose;

    @Lob
    private String supportTargetDetail;

    @Builder
    public LoanProductEntity(String seq, String productName, String handlingInstitution, String interestRateType, String interestRate, String loanLimit, String maxLoanTerm, String loanPurpose, String supportTargetDetail) {
        this.seq = seq;
        this.productName = productName;
        this.handlingInstitution = handlingInstitution;
        this.interestRateType = interestRateType;
        this.interestRate = interestRate;
        this.loanLimit = loanLimit;
        this.maxLoanTerm = maxLoanTerm;
        this.loanPurpose = loanPurpose;
        this.supportTargetDetail = supportTargetDetail;
    }
}
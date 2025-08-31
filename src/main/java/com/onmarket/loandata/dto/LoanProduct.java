package com.onmarket.loandata.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoanProduct {
    @JsonProperty("seq")
    private String seq;
    @JsonProperty("finPrdNm")
    private String productName;
    @JsonProperty("hdlInst")
    private String handlingInstitution;
    @JsonProperty("irtCtg")
    private String interestRateType;
    @JsonProperty("irt")
    private String interestRate;
    @JsonProperty("lnLmt")
    private String loanLimit;
    @JsonProperty("maxTotLnTrm")
    private String maxLoanTerm;
    @JsonProperty("usge")
    private String loanPurpose;
    @JsonProperty("suprTgtDtlCond")
    private String supportTargetDetail;
}
package com.onmarket.cardnews.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditLoanProductDto {
    private Long id; private String dclsMonth; private String finCoNo; private String korCoNm;
    private String finPrdtCd; private String finPrdtNm; private String joinWay; private String crdtPrdtType;
    private String crdtPrdtTypeNm; private String cbName; private String dclsStrtDay; private String dclsEndDay;
    private String finCoSubmDay;
}
package com.onmarket.cardnews.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditLoanOptionDto {
    private Long id; private String finCoNo; private String finPrdtCd; private String crdtLendRateType; // A/B/C/D
    private String crdtLendRateTypeNm; // 대출금리/기준금리/가산금리/가감조정금리
    private Double crdtGrad1; private Double crdtGrad4; private Double crdtGrad5; private Double crdtGrad6;
    private Double crdtGrad10; private Double crdtGrad11; private Double crdtGrad12; private Double crdtGrad13;
    private Double crdtGradAvg;
}
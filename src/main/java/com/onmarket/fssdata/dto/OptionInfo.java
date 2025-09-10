package com.onmarket.fssdata.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OptionInfo {
    @JsonProperty("fin_co_no")
    private String finCoNo;            // 금융회사 코드

    @JsonProperty("fin_prdt_cd")
    private String finPrdtCd;          // 금융상품 코드

    @JsonProperty("crdt_lend_rate_type")
    private String crdtLendRateType;   // 금리구분 코드

    @JsonProperty("crdt_lend_rate_type_nm")
    private String crdtLendRateTypeNm; // 금리구분

    @JsonProperty("crdt_grad_1")
    private Double crdtGrad1;          // 900점 초과

    @JsonProperty("crdt_grad_4")
    private Double crdtGrad4;          // 801~900점

    @JsonProperty("crdt_grad_5")
    private Double crdtGrad5;          // 701~800점

    @JsonProperty("crdt_grad_6")
    private Double crdtGrad6;          // 601~700점

    @JsonProperty("crdt_grad_10")
    private Double crdtGrad10;         // 501~600점

    @JsonProperty("crdt_grad_11")
    private Double crdtGrad11;         // 401~500점

    @JsonProperty("crdt_grad_12")
    private Double crdtGrad12;         // 301~400점

    @JsonProperty("crdt_grad_13")
    private Double crdtGrad13;         // 300점 이하

    @JsonProperty("crdt_grad_avg")
    private Double crdtGradAvg;        // 평균 금리
}
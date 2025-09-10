package com.onmarket.fssdata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BaseInfo {
    @JsonProperty("dcls_month")
    private String dclsMonth;          // 공시 제출월

    @JsonProperty("fin_co_no")
    private String finCoNo;            // 금융회사 코드

    @JsonProperty("fin_prdt_cd")
    private String finPrdtCd;          // 금융상품 코드

    @JsonProperty("crdt_prdt_type")
    private String crdtPrdtType;       // 대출종류 코드

    @JsonProperty("kor_co_nm")
    private String korCoNm;            // 금융회사명

    @JsonProperty("fin_prdt_nm")
    private String finPrdtNm;          // 금융상품명

    @JsonProperty("join_way")
    private String joinWay;            // 가입방법

    @JsonProperty("cb_name")
    private String cbName;             // CB 회사명

    @JsonProperty("crdt_prdt_type_nm")
    private String crdtPrdtTypeNm;     // 대출종류명

    @JsonProperty("dcls_strt_day")
    private String dclsStrtDay;        // 공시 시작일

    @JsonProperty("dcls_end_day")
    private String dclsEndDay;         // 공시 종료일

    @JsonProperty("fin_co_subm_day")
    private String finCoSubmDay;       // 금융회사 제출일
}

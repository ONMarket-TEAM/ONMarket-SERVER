package com.onmarket.loandata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class LoanApiResponse {
    @JsonProperty("header")
    private Header header;

    @JsonProperty("body")
    private Body body;

    @Data
    public static class Header {
        @JsonProperty("resultMsg")
        private String resultMsg;

        @JsonProperty("resultCode")
        private String resultCode;
    }

    @Data
    public static class Body {
        @JsonProperty("numOfRows")
        private Integer numOfRows;

        @JsonProperty("pageNo")
        private Integer pageNo;

        @JsonProperty("totalCount")
        private Integer totalCount;

        @JsonProperty("items")
        private Items items;
    }

    @Data
    public static class Items {
        @JsonProperty("item")
        private List<LoanItem> item;
    }

    @Data
    public static class LoanItem {
        @JsonProperty("age")
        private String age;

        @JsonProperty("age_39Blw")
        private String age39Blw;

        @JsonProperty("age_40Abnml")
        private String age40Abnml;

        @JsonProperty("age_60Abnml")
        private String age60Abnml;

        @JsonProperty("anin")
        private String anin;

        @JsonProperty("cnpl")
        private String cnpl;

        @JsonProperty("crdtSc")
        private String crdtSc;

        @JsonProperty("crdtSc_1")
        private String crdtSc1;

        @JsonProperty("crdtSc_2")
        private String crdtSc2;

        @JsonProperty("crdtSc_3")
        private String crdtSc3;

        @JsonProperty("crdtSc_4")
        private String crdtSc4;

        @JsonProperty("crdtSc_5")
        private String crdtSc5;

        @JsonProperty("crdtSc_6")
        private String crdtSc6;

        @JsonProperty("crdtSc_7")
        private String crdtSc7;

        @JsonProperty("crdtSc_8")
        private String crdtSc8;

        @JsonProperty("crdtSc_9")
        private String crdtSc9;

        @JsonProperty("crdtSc_0")
        private String crdtSc0;

        @JsonProperty("crdtSc_1_5")
        private String crdtSc15;

        @JsonProperty("crdtSc_6_0")
        private String crdtSc60;

        @JsonProperty("etcRefSbjc")
        private String etcRefSbjc;

        @JsonProperty("finPrdNm")
        private String finPrdNm;

        @JsonProperty("grnInst")
        private String grnInst;

        @JsonProperty("hdlInst")
        private String hdlInst;

        @JsonProperty("hdlInstDtlVw")
        private String hdlInstDtlVw;

        @JsonProperty("housAr")
        private String housAr;

        @JsonProperty("housHoldCnt")
        private String housHoldCnt;

        @JsonProperty("incm")
        private String incm;

        @JsonProperty("incmCnd")
        private String incmCnd;

        @JsonProperty("incmCndN")
        private String incmCndN;

        @JsonProperty("incmCndY")
        private String incmCndY;

        @JsonProperty("instCtg")
        private String instCtg;

        @JsonProperty("irt")
        private String irt;

        @JsonProperty("irtCtg")
        private String irtCtg;

        @JsonProperty("jnMthd")
        private String jnMthd;

        @JsonProperty("kinfaPrdEtc")
        private String kinfaPrdEtc;

        @JsonProperty("kinfaPrdYn")
        private String kinfaPrdYn;

        @JsonProperty("lnIcdcst")
        private String lnIcdcst;

        @JsonProperty("lnLmt")
        private String lnLmt;

        @JsonProperty("lnLmt_10000Abnml")
        private String lnLmt10000Abnml;

        @JsonProperty("lnLmt_1000Abnml")
        private String lnLmt1000Abnml;

        @JsonProperty("lnLmt_2000Abnml")
        private String lnLmt2000Abnml;

        @JsonProperty("lnLmt_3000Abnml")
        private String lnLmt3000Abnml;

        @JsonProperty("lnLmt_5000Abnml")
        private String lnLmt5000Abnml;

        @JsonProperty("lnTgtHous")
        private String lnTgtHous;

        @JsonProperty("maxDfrmTrm")
        private String maxDfrmTrm;

        @JsonProperty("maxRdptTrm")
        private String maxRdptTrm;

        @JsonProperty("maxTotLnTrm")
        private String maxTotLnTrm;

        @JsonProperty("ofrInstNm")
        private String ofrInstNm;

        @JsonProperty("ovItrYr")
        private String ovItrYr;

        @JsonProperty("prdCtg")
        private String prdCtg;

        @JsonProperty("prdOprPrid")
        private String prdOprPrid;

        @JsonProperty("prftAddIrtCond")
        private String prftAddIrtCond;

        @JsonProperty("rdptMthd")
        private String rdptMthd;

        @JsonProperty("rfrcCnpl")
        private String rfrCnpl;

        @JsonProperty("rltSite")
        private String rltSite;

        @JsonProperty("rpymdCfe")
        private String rpymdCfe;

        @JsonProperty("rsdArea")
        private String rsdArea;

        @JsonProperty("rsdAreaPamtEqltIstm")
        private String rsdAreaPamtEqltIstm;

        @JsonProperty("seq")
        private String seq;

        @JsonProperty("suprTgtDtlCond")
        private String suprTgtDtlCond;

        @JsonProperty("tgtFltr")
        private String tgtFltr;

        @JsonProperty("trgt")
        private String trgt;

        @JsonProperty("usge")
        private String usge;
    }
}
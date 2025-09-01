package com.onmarket.loandata.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;
import java.util.List;

@Data
@JacksonXmlRootElement(localName = "response")
public class XmlLoanApiResponse {

    @JacksonXmlProperty(localName = "header")
    private Header header;

    @JacksonXmlProperty(localName = "body")
    private Body body;

    @Data
    public static class Header {
        @JacksonXmlProperty(localName = "resultCode")
        private String resultCode;

        @JacksonXmlProperty(localName = "resultMsg")
        private String resultMsg;
    }

    @Data
    public static class Body {
        @JacksonXmlProperty(localName = "numOfRows")
        private Integer numOfRows;

        @JacksonXmlProperty(localName = "pageNo")
        private Integer pageNo;

        @JacksonXmlProperty(localName = "totalCount")
        private Integer totalCount;

        @JacksonXmlProperty(localName = "items")
        private Items items;
    }

    @Data
    public static class Items {
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "item")
        private List<XmlLoanItem> item;
    }

    @Data
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class XmlLoanItem {
        @JacksonXmlProperty(localName = "seq")
        private String seq;

        @JacksonXmlProperty(localName = "finprdnm")
        private String finPrdNm;

        @JacksonXmlProperty(localName = "lnlmt")
        private String lnLmt;

        @JacksonXmlProperty(localName = "lnlmt1000abnml")
        private String lnLmt1000Abnml;

        @JacksonXmlProperty(localName = "lnlmt2000abnml")
        private String lnLmt2000Abnml;

        @JacksonXmlProperty(localName = "lnlmt3000abnml")
        private String lnLmt3000Abnml;

        @JacksonXmlProperty(localName = "lnlmt5000abnml")
        private String lnLmt5000Abnml;

        @JacksonXmlProperty(localName = "lnlmt10000abnml")
        private String lnLmt10000Abnml;

        @JacksonXmlProperty(localName = "irtCtg")
        private String irtCtg;

        @JacksonXmlProperty(localName = "irt")
        private String irt;

        @JacksonXmlProperty(localName = "maxtotlntrm")
        private String maxTotLnTrm;

        @JacksonXmlProperty(localName = "maxdfrmtrm")
        private String maxDfrmTrm;

        @JacksonXmlProperty(localName = "maxrdpttrm")
        private String maxRdptTrm;

        @JacksonXmlProperty(localName = "rdptmthd")
        private String rdptMthd;

        @JacksonXmlProperty(localName = "usge")
        private String usge;

        @JacksonXmlProperty(localName = "trgt")
        private String trgt;

        @JacksonXmlProperty(localName = "instCtg")
        private String instCtg;

        @JacksonXmlProperty(localName = "ofrinstnm")
        private String ofrInstNm;

        @JacksonXmlProperty(localName = "rsdAreaPamtEqltIstm")
        private String rsdAreaPamtEqltIstm;

        @JacksonXmlProperty(localName = "suprtgtdtlcond")
        private String suprTgtDtlCond;

        // 오류에서 확인된 추가 필드들
        @JacksonXmlProperty(localName = "age")
        private String age;

        @JacksonXmlProperty(localName = "age39blw")
        private String age39Blw;

        @JacksonXmlProperty(localName = "incm")
        private String incm;

        @JacksonXmlProperty(localName = "hdlInst")
        private String hdlInst;

        @JacksonXmlProperty(localName = "jnMthd")
        private String jnMthd;
    }
}
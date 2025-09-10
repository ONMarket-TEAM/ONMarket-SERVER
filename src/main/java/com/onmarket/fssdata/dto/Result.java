package com.onmarket.fssdata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

import java.util.List;

@Data
public class Result {
    @JsonProperty("prdt_div")
    private String prdtDiv;

    @JsonProperty("total_count")
    private int totalCount;

    @JsonProperty("max_page_no")
    private int maxPageNo;

    @JsonProperty("now_page_no")
    private int nowPageNo;

    @JsonProperty("err_cd")
    private String errCd;

    @JsonProperty("err_msg")
    private String errMsg;

    @JsonProperty("baseList")
    private List<BaseInfo> baseList;

    @JsonProperty("optionList")
    private List<OptionInfo> optionList;
}

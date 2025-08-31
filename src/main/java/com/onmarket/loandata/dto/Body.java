package com.onmarket.loandata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Body {
    @JsonProperty("numOfRows")
    private int numOfRows;
    @JsonProperty("pageNo")
    private int pageNo;
    @JsonProperty("totalCount")
    private int totalCount;
    @JsonProperty("items")
    private Items items;
}
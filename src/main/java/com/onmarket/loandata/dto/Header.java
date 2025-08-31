package com.onmarket.loandata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Header {
    @JsonProperty("resultMsg")
    private String resultMsg;
    @JsonProperty("resultCode")
    private String resultCode;
}
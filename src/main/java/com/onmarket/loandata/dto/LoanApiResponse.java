
package com.onmarket.loandata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoanApiResponse {
    @JsonProperty("header")
    private Header header;
    @JsonProperty("body")
    private Body body;
}
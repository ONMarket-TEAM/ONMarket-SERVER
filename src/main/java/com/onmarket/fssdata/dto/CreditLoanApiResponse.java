package com.onmarket.fssdata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class CreditLoanApiResponse {
    @JsonProperty("result")
    private Result result;
}




package com.onmarket.loandata.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Items {
    @JsonProperty("item")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<LoanProduct> itemList;
}
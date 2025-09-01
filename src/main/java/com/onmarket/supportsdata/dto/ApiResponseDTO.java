package com.onmarket.supportsdata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ApiResponseDTO<T>  {

    @JsonProperty("currentCount")
    private int currentCount;

    @JsonProperty("totalCount")
    private int totalCount;

    @JsonProperty("page")
    private int page;

    @JsonProperty("perPage")
    private int perPage;

    @JsonProperty("data")
    private List<T> data;
}
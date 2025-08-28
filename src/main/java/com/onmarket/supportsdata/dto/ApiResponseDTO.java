package com.onmarket.supportsdata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ApiResponseDTO<T>  { // ◀◀-- 1. 클래스 선언부에 제네릭 타입 <T> 추가

    @JsonProperty("currentCount")
    private int currentCount;

    @JsonProperty("totalCount")
    private int totalCount;

    @JsonProperty("page")
    private int page;

    @JsonProperty("perPage")
    private int perPage;

    @JsonProperty("data")
    private List<T> data; // ◀◀-- 2. data 필드의 타입을 List<ServiceInfoDTO>에서 List<T>로 변경
}
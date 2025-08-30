package com.onmarket.business.dto;

import com.onmarket.business.domain.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "사업장 등록 요청 DTO")
public class BusinessRequest {

    @Schema(description = "산업 분야", example = "SERVICE")
    private Industry industry;

    @Schema(description = "사업장 유형", example = "CORPORATE")
    private BusinessType businessType;

    @Schema(description = "지역 코드 ID", example = "11011")
    private String regionCodeId;

    @Schema(description = "설립 연도", example = "2015")
    private Integer establishedYear;

    @Schema(description = "연 매출", example = "OVER_500M")
    private AnnualRevenue annualRevenue;

    @Schema(description = "직원 수", example = "50")
    private Integer employeeCount;
}

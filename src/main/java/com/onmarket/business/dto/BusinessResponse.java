package com.onmarket.business.dto;

import com.onmarket.business.domain.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "사업장 응답 DTO")
public class BusinessResponse {

    @Schema(description = "사업장 ID", example = "101")
    private Long businessId;

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

    @Schema(description = "사업장 상태", example = "ACTIVE")
    private BusinessStatus status;
}

package com.onmarket.business.dto;

import com.onmarket.business.domain.Business;
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

    @Schema(description = "사업장 이름", example = "OOO 주식회사")
    private String businessName;

    @Schema(description = "산업 분야", example = "SERVICE")
    private Industry industry;

    @Schema(description = "사업장 유형", example = "CORPORATE")
    private BusinessType businessType;

    /** 🔽 지역명/구명 직접 반환 */
    @Schema(description = "시/도명", example = "서울특별시")
    private String sidoName;

    @Schema(description = "시군구명", example = "종로구")
    private String sigunguName;

    @Schema(description = "설립 연도", example = "2015")
    private Integer establishedYear;

    @Schema(description = "연 매출", example = "OVER_500M")
    private AnnualRevenue annualRevenue;

    @Schema(description = "직원 수", example = "50")
    private Integer employeeCount;

    @Schema(description = "사업장 상태", example = "ACTIVE")
    private BusinessStatus status;

    public static BusinessResponse from(Business b) {
        return new BusinessResponse(
                b.getBusinessId(),
                b.getBusinessName(),
                b.getIndustry(),
                b.getBusinessType(),
                b.getSidoName(),
                b.getSigunguName(),
                b.getEstablishedYear(),
                b.getAnnualRevenue(),
                b.getEmployeeCount(),
                b.getStatus()
        );
    }
}

package com.onmarket.business.dto;

import com.onmarket.business.domain.enums.AnnualRevenue;
import com.onmarket.business.domain.enums.BusinessType;
import com.onmarket.business.domain.enums.Industry;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusinessUpdateRequest {
    private Industry industry;              // 업종
    private BusinessType businessType;      // 개인/법인
    private String regionCodeId;            // 지역 코드 (예: SEOUL-GWANGJIN)
    private Integer establishedYear;        // 설립연도
    private AnnualRevenue annualRevenue;    // 연매출 구간
    private Integer employeeCount;          // 상시 근로자 수
}

package com.onmarket.business.dto;

import com.onmarket.business.domain.enums.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BusinessResponse {
    private Long businessId;
    private Industry industry;
    private BusinessType businessType;
    private String regionCodeId;
    private Integer establishedYear;
    private AnnualRevenue annualRevenue;
    private Integer employeeCount;
    private BusinessStatus status;
}

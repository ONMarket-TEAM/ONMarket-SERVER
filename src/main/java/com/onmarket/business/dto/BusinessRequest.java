package com.onmarket.business.dto;

import com.onmarket.business.domain.enums.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusinessRequest {
    private Industry industry;
    private BusinessType businessType;
    private String regionCodeId;
    private Integer establishedYear;
    private AnnualRevenue annualRevenue;
    private Integer employeeCount;
}

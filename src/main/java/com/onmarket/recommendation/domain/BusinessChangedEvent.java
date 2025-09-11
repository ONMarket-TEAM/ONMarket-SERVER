package com.onmarket.recommendation.domain;

import com.onmarket.business.domain.Business;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BusinessChangedEvent {
    private Long businessId;
    private Business business;
    private BusinessChangeType changeType;
}

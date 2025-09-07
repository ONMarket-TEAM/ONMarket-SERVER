package com.onmarket.cardnews.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportServiceDto {
    private String serviceId; private String supportType; private String serviceName; private String servicePurposeSummary;
    private String supportTarget; private String selectionCriteria; private String supportContent; private String applicationMethod;
    private String detailUrl; private String departmentName; private String userCategory; private String servicePurpose;
    private String applicationDeadline; private String requiredDocuments; private String receptionAgencyName; private String contact;
    private String onlineApplicationUrl; private String laws; private String keywords;
}
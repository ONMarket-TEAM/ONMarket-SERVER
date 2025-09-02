package com.onmarket.supportsdata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ServiceInfoDTO {

    @JsonProperty("서비스ID")
    private String serviceId;

    @JsonProperty("지원유형")
    private String supportType;

    @JsonProperty("서비스명")
    private String serviceName;

    @JsonProperty("서비스목적요약")
    private String servicePurposeSummary;

    @JsonProperty("지원대상")
    private String supportTarget;

    @JsonProperty("선정기준")
    private String selectionCriteria;

    @JsonProperty("지원내용")
    private String supportContent;

    @JsonProperty("신청방법")
    private String applicationMethod;

    @JsonProperty("신청기한")
    private String applicationDeadline;

    @JsonProperty("상세조회URL")
    private String detailUrl;

    @JsonProperty("소관기관코드")
    private String departmentCode;

    @JsonProperty("소관기관명")
    private String departmentName;

    @JsonProperty("부서명")
    private String divisionName;

    @JsonProperty("조회수")
    private Integer viewCount;

    @JsonProperty("소관기관유형")
    private String departmentType;

    @JsonProperty("사용자구분")
    private String userCategory;

    @JsonProperty("서비스분야")
    private String serviceField;

    @JsonProperty("접수기관")
    private String receptionAgency;

    @JsonProperty("전화문의")
    private String contactPhone;

    @JsonProperty("등록일시")
    private String registrationDate;

    @JsonProperty("수정일시")
    private String lastModifiedDate;
}
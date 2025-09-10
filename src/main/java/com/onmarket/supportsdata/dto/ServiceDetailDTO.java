package com.onmarket.supportsdata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ServiceDetailDTO {

    @JsonProperty("서비스ID")
    private String serviceId;

    @JsonProperty("지원유형")
    private String supportType;

    @JsonProperty("서비스명")
    private String serviceName;

    @JsonProperty("서비스목적")
    private String servicePurpose;

    @JsonProperty("신청기한")
    private String applicationDeadline;

    @JsonProperty("지원대상")
    private String supportTarget;

    @JsonProperty("선정기준")
    private String selectionCriteria;

    @JsonProperty("지원내용")
    private String supportContent;

    @JsonProperty("신청방법")
    private String applicationMethod;

    @JsonProperty("구비서류")
    private String requiredDocuments;

    @JsonProperty("접수기관명")
    private String receptionAgencyName;

    @JsonProperty("문의처")
    private String contact;

    @JsonProperty("온라인신청사이트URL")
    private String onlineApplicationUrl;

    @JsonProperty("수정일시")
    private String lastModifiedDate;

    @JsonProperty("소관기관명")
    private String departmentName;

    @JsonProperty("행정규칙")
    private String administrativeRules;

    @JsonProperty("자치법규")
    private String municipalOrdinances;

    @JsonProperty("법령")
    private String laws;

    @JsonProperty("공무원확인구비서류")
    private String documentsForOfficialVerification;

    @JsonProperty("본인확인필요구비서류")
    private String documentsForPersonalVerification;
}
package com.onmarket.supportsdata.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "support_service")
public class SupportProduct {

    @Id
    @Column(name = "service_id")
    private String serviceId;

    // --- serviceList 에서 가져오는 정보 ---
    private String supportType;
    @Column(nullable = false)
    private String serviceName;
    @Column(length = 1000)
    private String servicePurposeSummary;
    @Column(length = 2000)
    private String supportTarget;
    private String selectionCriteria;
    @Column(length = 2000)
    private String supportContent;
    private String applicationMethod;
    @Column(length = 500)
    private String detailUrl;
    private String departmentName;
    private String userCategory; // 사용자구분 필드 추가

    // --- serviceDetail 에서 추가로 가져오는 정보 ---
    @Lob
    private String servicePurpose;
    private String applicationDeadline;
    @Lob
    private String requiredDocuments;
    private String receptionAgencyName;
    private String contact;
    private String onlineApplicationUrl;
    @Lob
    private String laws;

    // 키워드 저장을 위한 컬럼 추가
    @Column(name = "keywords", length = 1024)
    private String keywords;

    // --- SupportCondition 엔티티와 1:1 관계 설정 ---
    @OneToOne(mappedBy = "supportProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    private SupportCondition supportCondition;

    public void setSupportCondition(SupportCondition supportCondition) {
        this.supportCondition = supportCondition;
    }

    @Builder
    public SupportProduct(String serviceId, String supportType, String serviceName, String servicePurposeSummary, String supportTarget, String selectionCriteria, String supportContent, String applicationMethod, String detailUrl, String departmentName, String userCategory, String servicePurpose, String applicationDeadline, String requiredDocuments, String receptionAgencyName, String contact, String onlineApplicationUrl, String laws, String keywords) {
        this.serviceId = serviceId;
        this.supportType = supportType;
        this.serviceName = serviceName;
        this.servicePurposeSummary = servicePurposeSummary;
        this.supportTarget = supportTarget;
        this.selectionCriteria = selectionCriteria;
        this.supportContent = supportContent;
        this.applicationMethod = applicationMethod;
        this.detailUrl = detailUrl;
        this.departmentName = departmentName;
        this.userCategory = userCategory;
        this.servicePurpose = servicePurpose;
        this.applicationDeadline = applicationDeadline;
        this.requiredDocuments = requiredDocuments;
        this.receptionAgencyName = receptionAgencyName;
        this.contact = contact;
        this.onlineApplicationUrl = onlineApplicationUrl;
        this.laws = laws;
        this.keywords = keywords;
    }
}
package com.onmarket.supportsdata.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * 지원 서비스 정보 (지원사업)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "support_product")
public class SupportProduct {

    @Id
    @Column(name = "service_id")
    private String serviceId;                // 서비스 ID

    // --- serviceList 에서 가져오는 정보 ---
    private String supportType;              // 지원 유형 (예: 현금, 융자, 상담 등)
    @Column(nullable = false)
    private String serviceName;              // 서비스명
    @Column(length = 1000)
    private String servicePurposeSummary;    // 서비스 목적 요약
    @Column(length = 2000)
    private String supportTarget;            // 지원 대상
    private String selectionCriteria;        // 선정 기준
    @Column(length = 2000)
    private String supportContent;           // 지원 내용
    private String applicationMethod;        // 신청 방법
    @Column(length = 500)
    private String detailUrl;                // 상세 페이지 URL
    private String departmentName;           // 담당 부서
    private String userCategory;             // 사용자 구분 (소상공인, 법인 등)

    // --- serviceDetail 에서 추가로 가져오는 정보 ---
    @Lob
    private String servicePurpose;           // 서비스 목적 상세
    private String applicationDeadline;      // 신청 마감일
    @Lob
    private String requiredDocuments;        // 제출 서류
    private String receptionAgencyName;      // 접수 기관명
    private String contact;                  // 문의처
    private String onlineApplicationUrl;     // 온라인 신청 URL
    @Lob
    private String laws;                     // 관련 법령

    // 키워드 저장을 위한 컬럼 추가
    @Column(name = "keywords", length = 1024)
    private String keywords;                 // 검색 키워드

    // --- SupportCondition 엔티티와 1:1 관계 설정 ---
    @OneToOne(mappedBy = "supportProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    private SupportCondition supportCondition; // 연관된 지원 조건

    public void setSupportCondition(SupportCondition supportCondition) {
        this.supportCondition = supportCondition;
    }

    @Column(name = "cardnews_s3_key", length = 256)
    private String cardnewsS3Key;

    @Column(name = "cardnews_url", length = 512)
    private String cardnewsUrl;

    @Column(name = "cardnews_updated_at")
    private Instant cardnewsUpdatedAt;

    public void updateCardnews(String s3Key, String url, Instant updatedAt) {
        this.cardnewsS3Key = s3Key;
        this.cardnewsUrl = url;
        this.cardnewsUpdatedAt = updatedAt;

    }

    @Builder
    public SupportProduct(String serviceId, String supportType, String serviceName, String servicePurposeSummary,
                          String supportTarget, String selectionCriteria, String supportContent,
                          String applicationMethod, String detailUrl, String departmentName, String userCategory,
                          String servicePurpose, String applicationDeadline, String requiredDocuments,
                          String receptionAgencyName, String contact, String onlineApplicationUrl, String laws, String keywords) {
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

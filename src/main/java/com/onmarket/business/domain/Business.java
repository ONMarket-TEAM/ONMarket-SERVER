package com.onmarket.business.domain;

import com.onmarket.business.domain.enums.*;
import com.onmarket.common.domain.BaseTimeEntity;
import com.onmarket.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "business")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Business extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "business_id")
    private Long businessId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Industry industry;

    @Column(name = "business_name", length = 100, nullable = false)
    private String businessName;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false)
    private BusinessType businessType;

    @Column(name = "region_code_id", length = 20, nullable = false)
    private String regionCodeId;

    @Column(name = "established_year")
    private Integer establishedYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "annual_revenue", nullable = false)
    private AnnualRevenue annualRevenue;

    @Column(name = "employee_count")
    private Integer employeeCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusinessStatus status;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) this.status = BusinessStatus.ACTIVE;
    }

    /** 업종 변경 */
    public void changeIndustry(Industry industry) {
        this.industry = industry;
    }

    /** 사업 형태 변경 */
    public void changeBusinessType(BusinessType businessType) {
        this.businessType = businessType;
    }

    /** 지역 코드 변경 */
    public void changeRegion(String regionCodeId) {
        this.regionCodeId = regionCodeId;
    }

    /** 설립 연도 변경 */
    public void changeEstablishedYear(Integer establishedYear) {
        this.establishedYear = establishedYear;
    }

    /** 연매출 변경 */
    public void changeAnnualRevenue(AnnualRevenue annualRevenue) {
        this.annualRevenue = annualRevenue;
    }

    /** 직원 수 변경 */
    public void changeEmployeeCount(Integer employeeCount) {
        this.employeeCount = employeeCount;
    }

    /** 상태 변경 (ACTIVE, INACTIVE, CLOSED 등) */
    public void changeStatus(BusinessStatus status) {
        this.status = status;
    }
}

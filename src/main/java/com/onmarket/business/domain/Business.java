package com.onmarket.business.domain;

import com.onmarket.business.domain.enums.*;
import com.onmarket.common.domain.BaseTimeEntity;
import com.onmarket.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "business")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Business extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "business_id")
    private Long businessId;

    // Member와 N:1 관계 (FK: member_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Industry industry;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false)
    private BusinessType businessType;

    @Column(name = "region_code_id", length = 20, nullable = false)
    private String regionCodeId;

    @Column(name = "established_year")
    private Integer establishedYear; // YEAR 타입 → JPA에서는 int/Integer

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
}

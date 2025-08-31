package com.onmarket.fssdata.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "credit_loan_products")
@Data
public class CreditLoanProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dcls_month")
    private String dclsMonth;

    @Column(name = "fin_co_no")
    private String finCoNo;

    @Column(name = "kor_co_nm")
    private String korCoNm;

    @Column(name = "fin_prdt_cd")
    private String finPrdtCd;

    @Column(name = "fin_prdt_nm")
    private String finPrdtNm;

    @Column(name = "join_way", columnDefinition = "TEXT")
    private String joinWay;

    @Column(name = "crdt_prdt_type")
    private String crdtPrdtType;

    @Column(name = "crdt_prdt_type_nm")
    private String crdtPrdtTypeNm;

    @Column(name = "cb_name")
    private String cbName;

    @Column(name = "dcls_strt_day")
    private String dclsStrtDay;

    @Column(name = "dcls_end_day")
    private String dclsEndDay;

    @Column(name = "fin_co_subm_day")
    private String finCoSubmDay;

    @OneToMany(mappedBy = "creditLoanProduct", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<CreditLoanOption> options = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // == 연관관계 편의 메서드 == //
    public void addOption(CreditLoanOption option) {
        this.options.add(option);
        option.setCreditLoanProduct(this);
    }
}
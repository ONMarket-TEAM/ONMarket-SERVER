package com.onmarket.fssdata.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

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
}

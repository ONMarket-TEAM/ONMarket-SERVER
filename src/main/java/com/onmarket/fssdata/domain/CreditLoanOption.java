package com.onmarket.fssdata.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "credit_loan_options")
@Data
public class CreditLoanOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fin_co_no")
    private String finCoNo;

    @Column(name = "fin_prdt_cd")
    private String finPrdtCd;

    @Column(name = "crdt_lend_rate_type")
    private String crdtLendRateType;

    @Column(name = "crdt_lend_rate_type_nm")
    private String crdtLendRateTypeNm;

    @Column(name = "crdt_grad_1")
    private Double crdtGrad1;

    @Column(name = "crdt_grad_4")
    private Double crdtGrad4;

    @Column(name = "crdt_grad_5")
    private Double crdtGrad5;

    @Column(name = "crdt_grad_6")
    private Double crdtGrad6;

    @Column(name = "crdt_grad_10")
    private Double crdtGrad10;

    @Column(name = "crdt_grad_11")
    private Double crdtGrad11;

    @Column(name = "crdt_grad_12")
    private Double crdtGrad12;

    @Column(name = "crdt_grad_13")
    private Double crdtGrad13;

    @Column(name = "crdt_grad_avg")
    private Double crdtGradAvg;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

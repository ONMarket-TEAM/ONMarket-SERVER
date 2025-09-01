package com.onmarket.fssdata.domain;

import com.onmarket.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Entity
@Table(name = "credit_loan_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditLoanOption extends BaseTimeEntity {
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private CreditLoanProduct creditLoanProduct;

    @Builder
    public CreditLoanOption(String finCoNo, String finPrdtCd, String crdtLendRateType,
                            String crdtLendRateTypeNm, Double crdtGrad1, Double crdtGrad4,
                            Double crdtGrad5, Double crdtGrad6, Double crdtGrad10,
                            Double crdtGrad11, Double crdtGrad12, Double crdtGrad13,
                            Double crdtGradAvg, CreditLoanProduct creditLoanProduct) {
        this.finCoNo = finCoNo;
        this.finPrdtCd = finPrdtCd;
        this.crdtLendRateType = crdtLendRateType;
        this.crdtLendRateTypeNm = crdtLendRateTypeNm;
        this.crdtGrad1 = crdtGrad1;
        this.crdtGrad4 = crdtGrad4;
        this.crdtGrad5 = crdtGrad5;
        this.crdtGrad6 = crdtGrad6;
        this.crdtGrad10 = crdtGrad10;
        this.crdtGrad11 = crdtGrad11;
        this.crdtGrad12 = crdtGrad12;
        this.crdtGrad13 = crdtGrad13;
        this.crdtGradAvg = crdtGradAvg;
        this.creditLoanProduct = creditLoanProduct;
    }

    // 비즈니스 로직이 필요한 경우 메서드로 안전하게 값 변경
    public void updateCreditGrades(Double grad1, Double grad4, Double grad5, Double grad6,
                                   Double grad10, Double grad11, Double grad12, Double grad13, Double gradAvg) {
        this.crdtGrad1 = grad1;
        this.crdtGrad4 = grad4;
        this.crdtGrad5 = grad5;
        this.crdtGrad6 = grad6;
        this.crdtGrad10 = grad10;
        this.crdtGrad11 = grad11;
        this.crdtGrad12 = grad12;
        this.crdtGrad13 = grad13;
        this.crdtGradAvg = gradAvg;
    }

    public void updateCreditLoanProduct(CreditLoanProduct creditLoanProduct) {
        this.creditLoanProduct = creditLoanProduct;
    }
}
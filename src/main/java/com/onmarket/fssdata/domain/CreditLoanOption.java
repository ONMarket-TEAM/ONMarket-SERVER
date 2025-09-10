package com.onmarket.fssdata.domain;

import com.onmarket.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

/**
 * 신용대출 상품의 금리 옵션 정보
 * 각 상품별로 신용등급에 따른 금리 조건을 관리
 */
@Entity
@Table(name = "credit_loan_option")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditLoanOption extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 옵션 고유 ID

    @Column(name = "fin_co_no")
    private String finCoNo; // 금융회사 코드

    @Column(name = "fin_prdt_cd")
    private String finPrdtCd; // 금융상품 코드 (상품 식별자)

    @Column(name = "crdt_lend_rate_type")
    private String crdtLendRateType; // 금리 유형 (변동/고정)

    @Column(name = "crdt_lend_rate_type_nm")
    private String crdtLendRateTypeNm; // 금리 유형명

    // 신용등급별 금리 정보 (%)
    @Column(name = "crdt_grad_1")
    private Double crdtGrad1; // 1등급 금리

    @Column(name = "crdt_grad_4")
    private Double crdtGrad4; // 4등급 금리

    @Column(name = "crdt_grad_5")
    private Double crdtGrad5; // 5등급 금리

    @Column(name = "crdt_grad_6")
    private Double crdtGrad6; // 6등급 금리

    @Column(name = "crdt_grad_10")
    private Double crdtGrad10; // 10등급 금리

    @Column(name = "crdt_grad_11")
    private Double crdtGrad11; // 11등급 금리

    @Column(name = "crdt_grad_12")
    private Double crdtGrad12; // 12등급 금리

    @Column(name = "crdt_grad_13")
    private Double crdtGrad13; // 13등급 금리

    @Column(name = "crdt_grad_avg")
    private Double crdtGradAvg; // 평균 금리

    // 상품과의 연관관계 (fin_prdt_cd로 연결)
    @ManyToOne(fetch = FetchType.LAZY)
//  @JoinColumn(name = "fin_prdt_cd", referencedColumnName = "fin_prdt_cd", insertable = false, updatable = false)
    @JoinColumn(name = "credit_loan_product_id")  // FK를 별도 컬럼으로 설정
    private CreditLoanProduct creditLoanProduct; // 연관된 신용대출 상품

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

    /**
     * 신용등급별 금리 정보 업데이트
     */
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

    /**
     * 연관된 상품 정보 업데이트
     */
    public void updateCreditLoanProduct(CreditLoanProduct creditLoanProduct) {
        this.creditLoanProduct = creditLoanProduct;
    }
}
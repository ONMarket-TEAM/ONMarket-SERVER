package com.onmarket.fssdata.domain;

import com.onmarket.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "credit_loan_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditLoanProduct extends BaseTimeEntity {
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

    @Builder
    public CreditLoanProduct(String dclsMonth, String finCoNo, String korCoNm, String finPrdtCd,
                             String finPrdtNm, String joinWay, String crdtPrdtType, String crdtPrdtTypeNm,
                             String cbName, String dclsStrtDay, String dclsEndDay, String finCoSubmDay) {
        this.dclsMonth = dclsMonth;
        this.finCoNo = finCoNo;
        this.korCoNm = korCoNm;
        this.finPrdtCd = finPrdtCd;
        this.finPrdtNm = finPrdtNm;
        this.joinWay = joinWay;
        this.crdtPrdtType = crdtPrdtType;
        this.crdtPrdtTypeNm = crdtPrdtTypeNm;
        this.cbName = cbName;
        this.dclsStrtDay = dclsStrtDay;
        this.dclsEndDay = dclsEndDay;
        this.finCoSubmDay = finCoSubmDay;
    }

    // 비즈니스 로직 - 상품 정보 업데이트
    public void updateProductInfo(String dclsMonth, String korCoNm, String finPrdtNm,
                                  String joinWay, String crdtPrdtType, String crdtPrdtTypeNm,
                                  String cbName, String dclsStrtDay, String dclsEndDay, String finCoSubmDay) {
        this.dclsMonth = dclsMonth;
        this.korCoNm = korCoNm;
        this.finPrdtNm = finPrdtNm;
        this.joinWay = joinWay;
        this.crdtPrdtType = crdtPrdtType;
        this.crdtPrdtTypeNm = crdtPrdtTypeNm;
        this.cbName = cbName;
        this.dclsStrtDay = dclsStrtDay;
        this.dclsEndDay = dclsEndDay;
        this.finCoSubmDay = finCoSubmDay;
    }

    // == 연관관계 편의 메서드 == //
    public void addOption(CreditLoanOption option) {
        this.options.add(option);
        option.updateCreditLoanProduct(this);
    }
}
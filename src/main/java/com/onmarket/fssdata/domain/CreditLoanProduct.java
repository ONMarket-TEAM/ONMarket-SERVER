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

/**
 * 신용대출 상품 정보
 * 금융감독원 API에서 제공하는 개인신용대출 상품의 기본 정보를 관리
 */
@Entity
@Table(name = "credit_loan_product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditLoanProduct extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 상품 고유 ID

    @Column(name = "dcls_month")
    private String dclsMonth; // 공시 월 (YYYYMM)

    @Column(name = "fin_co_no")
    private String finCoNo; // 금융회사 코드

    @Column(name = "kor_co_nm")
    private String korCoNm; // 금융회사명 (한국어)

    @Column(name = "fin_prdt_cd")
    private String finPrdtCd; // 금융상품 코드 (상품 식별자)

    @Column(name = "fin_prdt_nm")
    private String finPrdtNm; // 금융상품명

    @Column(name = "join_way", columnDefinition = "TEXT")
    private String joinWay; // 가입 방법 (온라인, 영업점 등)

    @Column(name = "crdt_prdt_type")
    private String crdtPrdtType; // 신용대출 상품 유형 코드

    @Column(name = "crdt_prdt_type_nm")
    private String crdtPrdtTypeNm; // 신용대출 상품 유형명

    @Column(name = "cb_name")
    private String cbName; // 신용평가회사명

    @Column(name = "dcls_strt_day")
    private String dclsStrtDay; // 공시 시작일 (YYYYMMDD)

    @Column(name = "dcls_end_day")
    private String dclsEndDay; // 공시 종료일 (YYYYMMDD)

    @Column(name = "fin_co_subm_day")
    private String finCoSubmDay; // 금융회사 제출일 (YYYYMMDD)

    @Column(name = "rlt_site")
    private String rltSite; // 금융회사 홈페이지 URL

    // 해당 상품의 금리 옵션들 (양방향 연관관계)
    @OneToMany(mappedBy = "creditLoanProduct", fetch = FetchType.LAZY)
    @ToString.Exclude  // toString 무한루프 방지
    @EqualsAndHashCode.Exclude  // equals/hashCode 무한루프 방지
    private List<CreditLoanOption> options = new ArrayList<>(); // 상품별 금리 옵션 목록

    @Builder
    public CreditLoanProduct(String dclsMonth, String finCoNo, String korCoNm, String finPrdtCd,
                             String finPrdtNm, String joinWay, String crdtPrdtType, String crdtPrdtTypeNm,
                             String cbName, String dclsStrtDay, String dclsEndDay, String finCoSubmDay, String rltSite) {
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
        this.rltSite = rltSite;

    }

    /**
     * 상품 정보 업데이트 (공시 정보 변경 시 사용)
     */
    public void updateProductInfo(String dclsMonth, String korCoNm, String finPrdtNm,
                                  String joinWay, String crdtPrdtType, String crdtPrdtTypeNm,
                                  String cbName, String dclsStrtDay, String dclsEndDay, String finCoSubmDay, String rltSite) {
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
        this.rltSite = rltSite;

    }

    /**
     * 연관관계 편의 메서드 - 옵션 추가
     * 양방향 연관관계를 안전하게 설정
     */
    public void addOption(CreditLoanOption option) {
        this.options.add(option);
        option.updateCreditLoanProduct(this);
    }
}
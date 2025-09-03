package com.onmarket.fssdata.domain;

import com.onmarket.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "credit_loan_product",
        uniqueConstraints = {
                // fin_prdt_cd가 외래키 참조 대상이 되므로 UNIQUE 필수
                @UniqueConstraint(
                        name = "uk_credit_loan_product_fin_prdt_cd",
                        columnNames = "fin_prdt_cd"
                )
        }
)
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

    // ✅ 외래키 참조 대상 컬럼(유니크)
    @Column(name = "fin_prdt_cd", nullable = false, length = 100)
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

    @Column(name = "rlt_site")
    private String rltSite;

    @Column(name = "cardnews_s3_key", length = 256)
    private String cardnewsS3Key;

    @Column(name = "cardnews_url", length = 512)
    private String cardnewsUrl;

    @Column(name = "cardnews_updated_at")
    private Instant cardnewsUpdatedAt;

    @OneToMany(mappedBy = "creditLoanProduct", fetch = FetchType.LAZY)
    private List<CreditLoanOption> options = new ArrayList<>();

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

    public void updateProductInfo(String dclsMonth, String korCoNm, String finPrdtNm,
                                  String joinWay, String crdtPrdtType, String crdtPrdtTypeNm,
                                  String cbName, String dclsStrtDay, String dclsEndDay,
                                  String finCoSubmDay, String rltSite) {
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

    public void addOption(CreditLoanOption option) {
        this.options.add(option);
        option.updateCreditLoanProduct(this);
    }

    public void updateCardnews(String s3Key, String url, Instant updatedAt) {
        this.cardnewsS3Key = s3Key;
        this.cardnewsUrl = url;
        this.cardnewsUpdatedAt = updatedAt;
    }
}
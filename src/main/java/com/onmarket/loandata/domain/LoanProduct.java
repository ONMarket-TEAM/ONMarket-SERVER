package com.onmarket.loandata.domain;

import com.onmarket.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Entity
@Table(name = "loan_product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoanProduct extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- 기본 식별 정보 ---
    @Column(name = "seq")
    private String sequence; // 고유 순번

    @Column(name = "fin_prd_nm")
    private String productName; // 금융 상품명

    // --- 대출 용도 및 대상 ---
    @Column(name = "usge")
    private String usage; // 대출 용도 (예: 생계자금)

    @Column(name = "trgt")
    private String target; // 대출 대상 (예: 근로자)
//
//    @Column(name = "tgt_fltr")
//    private String targetFilter; // 대상 필터

    @Column(name = "inst_ctg")
    private String institutionCategory; // 금융기관 분류

    @Column(name = "ofr_inst_nm")
    private String offeringInstitution; // 상품 제공 기관명

//    @Column(name = "grn_inst")
//    private String guaranteeInstitution; // 보증 기관

//    @Column(name = "hdl_inst")
//    private String handlingInstitution; // 취급 기관
//
//    @Column(name = "hdl_inst_dtl_vw", columnDefinition = "TEXT")
//    private String handlingInstitutionDetailView; // 취급 기관 상세

    // --- 상환 관련 ---
    @Column(name = "rdpt_mthd")
    private String repaymentMethod; // 상환 방식

    @Column(name = "irt_ctg")
    private String interestCategory; // 금리 구분

    @Column(name = "irt")
    private String interestRate; // 금리

    // --- 대출 한도 및 기간 ---
    @Column(name = "ln_lmt")
    private String loanLimit; // 총 한도

    @Column(name = "ln_lmt_1000_abnml")
    private String limitOver1000; // 1,000만원 이상 대출 가능 여부

    @Column(name = "ln_lmt_2000_abnml")
    private String limitOver2000;

    @Column(name = "ln_lmt_3000_abnml")
    private String limitOver3000;

    @Column(name = "ln_lmt_5000_abnml")
    private String limitOver5000;

    @Column(name = "ln_lmt_10000_abnml")
    private String limitOver10000;

    @Column(name = "max_tot_ln_trm")
    private String maxTotalTerm; // 총 최대 대출 기간

    @Column(name = "max_dfrm_trm")
    private String maxDeferredTerm; // 최대 거치 기간

    @Column(name = "max_rdpt_trm")
    private String maxRepaymentTerm; // 최대 상환 기간

    // --- 연령 조건 ---
    @Column(name = "age")
    private String age; // 연령 조건

    @Column(name = "age_39_blw")
    private String ageBelow39; // 39세 이하 가능 여부

//    @Column(name = "age_40_abnml")
//    private String ageAbove40; // 40세 이상 가능 여부
//
//    @Column(name = "age_60_abnml")
//    private String ageAbove60; // 60세 이상 가능 여부

    // --- 소득 조건 ---
//    @Column(name = "incm", columnDefinition = "TEXT")
//    private String income; // 소득 조건
//
//    @Column(name = "incm_cnd", columnDefinition = "TEXT")
//    private String incomeCondition; // 소득 조건 전체
//
//    @Column(name = "incm_cnd_y", columnDefinition = "TEXT")
//    private String incomeConditionYes; // 소득 조건 만족
//
//    @Column(name = "incm_cnd_n", columnDefinition = "TEXT")
//    private String incomeConditionNo; // 소득 조건 불만족

    // --- 신용 조건 ---
//    @Column(name = "crdt_sc")
//    private String creditScore; // 기본 신용 등급 조건

//    @Column(name = "crdt_sc_0")
//    private String creditScore0;
//
//    @Column(name = "crdt_sc_1")
//    private String creditScore1;
//
//    @Column(name = "crdt_sc_2")
//    private String creditScore2;
//
//    @Column(name = "crdt_sc_3")
//    private String creditScore3;
//
//    @Column(name = "crdt_sc_4")
//    private String creditScore4;
//
//    @Column(name = "crdt_sc_5")
//    private String creditScore5;
//
//    @Column(name = "crdt_sc_6")
//    private String creditScore6;
//
//    @Column(name = "crdt_sc_7")
//    private String creditScore7;
//
//    @Column(name = "crdt_sc_8")
//    private String creditScore8;
//
//    @Column(name = "crdt_sc_9")
//    private String creditScore9;
//
//    @Column(name = "crdt_sc_1_5")
//    private String creditScore15;
//
//    @Column(name = "crdt_sc_6_0")
//    private String creditScore60;

    // --- 기타 조건 및 정보 ---
//    @Column(name = "anin")
//    private String anin; // 보증 관련 정보

//    @Column(name = "cnpl")
//    private String contactInfoDetail; // 상담 연락처

//    @Column(name = "rfrc_cnpl")
//    private String referenceContact; // 참고 연락처
//
//    @Column(name = "hous_ar")
//    private String housingArea; // 주거 면적
//
//    @Column(name = "hous_hold_cnt")
//    private String householdCount; // 가구 인원
//
//    @Column(name = "ln_tgt_hous")
//    private String loanTargetHousing; // 대출 대상 주택

    @Column(name = "supr_tgt_dtl_cond", columnDefinition = "TEXT")
    private String specialTargetConditions; // 특별 대상 조건

//    @Column(name = "etc_ref_sbjc", columnDefinition = "TEXT")
//    private String otherReference; // 기타 참고사항

//    @Column(name = "rpymd_cfe")
//    private String repaymentFee; // 상환 수수료

//    @Column(name = "ln_icdcst")
//    private String loanInsuranceCost; // 보증료율

//    @Column(name = "prd_ctg")
//    private String productCategory; // 상품 카테고리
//
//    @Column(name = "prd_opr_prid")
//    private String productOperationPeriod; // 운영 기간

//    @Column(name = "kinfa_prd_yn")
//    private String isKinfaProduct; // 근로복지공단 관련 여부 (Y/N)
//
//    @Column(name = "kinfa_prd_etc")
//    private String kinfaProductEtc; // 추가 정보

    @Column(name = "rlt_site")
    private String relatedSite; // 관련 사이트 URL


    @Column(name = "keywords", length = 512)
    private String keywords;

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public void setRelatedSite(String relatedSite) {
        this.relatedSite = relatedSite;
    }

    @Builder
    public LoanProduct(String sequence, String productName, String usage, String target,
                       String targetFilter, String institutionCategory, String offeringInstitution,
                       String guaranteeInstitution, String handlingInstitution, String handlingInstitutionDetailView,
                       String repaymentMethod, String interestCategory, String interestRate,
                       String loanLimit, String limitOver1000, String limitOver2000, String limitOver3000,
                       String limitOver5000, String limitOver10000, String maxTotalTerm, String maxDeferredTerm,
                       String maxRepaymentTerm, String age, String ageBelow39, String ageAbove40, String ageAbove60,
                       String income, String incomeCondition, String incomeConditionYes, String incomeConditionNo,
                       String creditScore, String specialTargetConditions, String otherReference,
                       String repaymentFee, String loanInsuranceCost, String productCategory,
                       String productOperationPeriod, String isKinfaProduct, String kinfaProductEtc, String relatedSite, String keywords) {
        this.sequence = sequence;
        this.productName = productName;
        this.usage = usage;
        this.target = target;
//        this.targetFilter = targetFilter;
        this.institutionCategory = institutionCategory;
        this.offeringInstitution = offeringInstitution;
//        this.guaranteeInstitution = guaranteeInstitution;
//        this.handlingInstitution = handlingInstitution;
//        this.handlingInstitutionDetailView = handlingInstitutionDetailView;
        this.repaymentMethod = repaymentMethod;
        this.interestCategory = interestCategory;
        this.interestRate = interestRate;
        this.loanLimit = loanLimit;
        this.limitOver1000 = limitOver1000;
        this.limitOver2000 = limitOver2000;
        this.limitOver3000 = limitOver3000;
        this.limitOver5000 = limitOver5000;
        this.limitOver10000 = limitOver10000;
        this.maxTotalTerm = maxTotalTerm;
        this.maxDeferredTerm = maxDeferredTerm;
        this.maxRepaymentTerm = maxRepaymentTerm;
        this.age = age;
        this.ageBelow39 = ageBelow39;
//        this.ageAbove40 = ageAbove40;
//        this.ageAbove60 = ageAbove60;
//        this.income = income;
//        this.incomeCondition = incomeCondition;
//        this.incomeConditionYes = incomeConditionYes;
//        this.incomeConditionNo = incomeConditionNo;
//        this.creditScore = creditScore;
        this.specialTargetConditions = specialTargetConditions;
//        this.otherReference = otherReference;
//        this.repaymentFee = repaymentFee;
//        this.loanInsuranceCost = loanInsuranceCost;
//        this.productCategory = productCategory;
//        this.productOperationPeriod = productOperationPeriod;
//        this.isKinfaProduct = isKinfaProduct;
//        this.kinfaProductEtc = kinfaProductEtc;
        this.relatedSite = relatedSite;
        this.keywords = keywords;
    }


    // 비즈니스 로직 - XML 데이터로부터 상품 정보 업데이트
    public void updateFromXmlData(String sequence, String productName, String loanLimit,
                                  String limitOver1000, String limitOver2000, String limitOver3000,
                                  String limitOver5000, String limitOver10000, String interestCategory,
                                  String interestRate, String maxTotalTerm, String maxDeferredTerm,
                                  String maxRepaymentTerm, String repaymentMethod, String usage,
                                  String target, String institutionCategory, String offeringInstitution,
                                  String specialTargetConditions, String age, String ageBelow39,
                                  String income, String handlingInstitution) {
        this.sequence = sequence;
        this.productName = productName;
        this.loanLimit = loanLimit;
        this.limitOver1000 = limitOver1000;
        this.limitOver2000 = limitOver2000;
        this.limitOver3000 = limitOver3000;
        this.limitOver5000 = limitOver5000;
        this.limitOver10000 = limitOver10000;
        this.interestCategory = interestCategory;
        this.interestRate = interestRate;
        this.maxTotalTerm = maxTotalTerm;
        this.maxDeferredTerm = maxDeferredTerm;
        this.maxRepaymentTerm = maxRepaymentTerm;
        this.repaymentMethod = repaymentMethod;
        this.usage = usage;
        this.target = target;
        this.institutionCategory = institutionCategory;
        this.offeringInstitution = offeringInstitution;
        this.specialTargetConditions = specialTargetConditions;
        this.age = age;
        this.ageBelow39 = ageBelow39;
//        this.income = income;
//        this.handlingInstitution = handlingInstitution;


    }
}
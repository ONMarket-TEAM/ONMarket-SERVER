package com.onmarket.supportsdata.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지원 조건 정보 (지원대상 조건)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "support_condition")
public class SupportCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 조건 ID

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "support_product_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_support_condition_product")
    )
    private SupportProduct supportProduct; // 연관된 지원 서비스 (FK: support_product.id)

    // --- JA 코드 필드들 ---
    private String genderMale;          // 남성 지원 여부 (Y/N)
    private String genderFemale;        // 여성 지원 여부 (Y/N)
    private Integer ageStart;           // 연령 시작
    private Integer ageEnd;             // 연령 끝
    private String incomeBracket1;      // 소득구간 1
    private String incomeBracket2;      // 소득구간 2
    private String incomeBracket3;      // 소득구간 3
    private String incomeBracket4;      // 소득구간 4
    private String incomeBracket5;      // 소득구간 5
    private String jobEmployee;         // 재직자 여부 (Y/N)
    private String jobSeeker;           // 구직자 여부 (Y/N)
    private String householdSinglePerson; // 1인 가구 (Y/N)
    private String householdMultiChild;   // 다자녀 가구 (Y/N)
    private String householdNoHome;       // 무주택 (Y/N)
    private String businessProspective;   // 창업기업 (Y/N)
    private String businessOperating;     // 운영기업 (Y/N)
    private String businessStruggling;    // 어려움 기업 (Y/N)

    // 양방향 연관관계 편의 메서드
    public void setSupportProduct(SupportProduct supportProduct) {
        this.supportProduct = supportProduct;
        if (supportProduct.getSupportCondition() != this) {
            supportProduct.setSupportCondition(this);
        }
    }

    @Builder
    public SupportCondition(String genderMale, String genderFemale, Integer ageStart, Integer ageEnd,
                            String incomeBracket1, String incomeBracket2, String incomeBracket3, String incomeBracket4, String incomeBracket5,
                            String jobEmployee, String jobSeeker,
                            String householdSinglePerson, String householdMultiChild, String householdNoHome,
                            String businessProspective, String businessOperating, String businessStruggling) {
        this.genderMale = genderMale;
        this.genderFemale = genderFemale;
        this.ageStart = ageStart;
        this.ageEnd = ageEnd;
        this.incomeBracket1 = incomeBracket1;
        this.incomeBracket2 = incomeBracket2;
        this.incomeBracket3 = incomeBracket3;
        this.incomeBracket4 = incomeBracket4;
        this.incomeBracket5 = incomeBracket5;
        this.jobEmployee = jobEmployee;
        this.jobSeeker = jobSeeker;
        this.householdSinglePerson = householdSinglePerson;
        this.householdMultiChild = householdMultiChild;
        this.householdNoHome = householdNoHome;
        this.businessProspective = businessProspective;
        this.businessOperating = businessOperating;
        this.businessStruggling = businessStruggling;
    }
}

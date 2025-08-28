package com.onmarket.supportsdata.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "support_condition")
public class SupportCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", unique = true)
    private SupportService supportService;

    // --- JA 코드 필드들 ---
    private String genderMale;
    private String genderFemale;
    private Integer ageStart;
    private Integer ageEnd;
    private String incomeBracket1;
    private String incomeBracket2;
    private String incomeBracket3;
    private String incomeBracket4;
    private String incomeBracket5;
    private String jobEmployee;
    private String jobSeeker;
    private String householdSinglePerson;
    private String householdMultiChild;
    private String householdNoHome;
    private String businessProspective;
    private String businessOperating;
    private String businessStruggling;
    // ...DTO에 추가한 모든 필드들을 여기에 추가...


    public void setSupportService(SupportService supportService) {
        this.supportService = supportService;
        if (supportService.getSupportCondition() != this) {
            supportService.setSupportCondition(this);
        }
    }

    @Builder
    public SupportCondition(String genderMale, String genderFemale, Integer ageStart, Integer ageEnd, String incomeBracket1, String incomeBracket2, String incomeBracket3, String incomeBracket4, String incomeBracket5, String jobEmployee, String jobSeeker, String householdSinglePerson, String householdMultiChild, String householdNoHome, String businessProspective, String businessOperating, String businessStruggling) {
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
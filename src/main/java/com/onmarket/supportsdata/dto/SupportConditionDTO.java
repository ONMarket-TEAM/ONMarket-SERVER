package com.onmarket.supportsdata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SupportConditionDTO {

    @JsonProperty("서비스ID")
    private String serviceId;

    @JsonProperty("서비스명")
    private String serviceName;

    // --- 성별/연령 ---
    @JsonProperty("JA0101")
    private String genderMale; // 남성

    @JsonProperty("JA0102")
    private String genderFemale; // 여성

    @JsonProperty("JA0110")
    private Integer ageStart; // 대상연령(시작)

    @JsonProperty("JA0111")
    private Integer ageEnd; // 대상연령(종료)

    // --- 소득 ---
    @JsonProperty("JA0201")
    private String incomeBracket1; // 중위소득 0~50%

    @JsonProperty("JA0202")
    private String incomeBracket2; // 중위소득 51~75%

    @JsonProperty("JA0203")
    private String incomeBracket3; // 중위소득 76~100%

    @JsonProperty("JA0204")
    private String incomeBracket4; // 중위소득 101~200%

    @JsonProperty("JA0205")
    private String incomeBracket5; // 중위소득 200% 초과

    // --- 생애주기 및 직업 ---
    @JsonProperty("JA0301")
    private String lifeCyclePreParent; // 예비부모/난임

    @JsonProperty("JA0302")
    private String lifeCyclePregnant; // 임산부

    @JsonProperty("JA0303")
    private String lifeCycleBirthAdoption; // 출산/입양

    @JsonProperty("JA0313")
    private String jobFarmer; // 농업인

    @JsonProperty("JA0314")
    private String jobFisher; // 어업인

    @JsonProperty("JA0315")
    private String jobLivestockFarmer; // 축산업인

    @JsonProperty("JA0316")
    private String jobForestryWorker; // 임업인

    @JsonProperty("JA0317")
    private String jobElementaryStudent; // 초등학생

    @JsonProperty("JA0318")
    private String jobMiddleSchoolStudent; // 중학생

    @JsonProperty("JA0319")
    private String jobHighSchoolStudent; // 고등학생

    @JsonProperty("JA0320")
    private String jobUniversityStudent; // 대학생/대학원생

    @JsonProperty("JA0322")
    private String jobNotApplicable; // 해당사항없음

    @JsonProperty("JA0326")
    private String jobEmployee; // 근로자/직장인

    @JsonProperty("JA0327")
    private String jobSeeker; // 구직자/실업자

    @JsonProperty("JA0328")
    private String specialConditionDisabled; // 장애인

    @JsonProperty("JA0329")
    private String specialConditionNationalMerit; // 국가보훈대상자

    @JsonProperty("JA0330")
    private String specialConditionPatient; // 질병/질환자

    // --- 가구 특성 ---
    @JsonProperty("JA0401")
    private String householdMulticultural; // 다문화가족

    @JsonProperty("JA0402")
    private String householdNorthKoreanDefector; // 북한이탈주민

    @JsonProperty("JA0403")
    private String householdSingleParent; // 한부모가정/조손가정

    @JsonProperty("JA0404")
    private String householdSinglePerson; // 1인가구

    @JsonProperty("JA0410")
    private String householdNotApplicable; // 해당사항없음

    @JsonProperty("JA0411")
    private String householdMultiChild; // 다자녀가구

    @JsonProperty("JA0412")
    private String householdNoHome; // 무주택세대

    @JsonProperty("JA0413")
    private String householdNewResident; // 신규전입

    @JsonProperty("JA0414")
    private String householdExtendedFamily; // 확대가족

    // --- 소상공인/기업 특성 ---
    @JsonProperty("JA1101")
    private String businessProspective; // 예비창업자

    @JsonProperty("JA1102")
    private String businessOperating; // 영업중

    @JsonProperty("JA1103")
    private String businessStruggling; // 생계곤란/폐업예정자

    @JsonProperty("JA1201")
    private String businessTypeRestaurant; // 음식점업

    @JsonProperty("JA1202")
    private String businessTypeManufacturing; // 제조업

    @JsonProperty("JA1299")
    private String businessTypeEtc; // 기타업종

    @JsonProperty("JA2101")
    private String corpTypeSME; // 중소기업

    @JsonProperty("JA2102")
    private String corpTypeSocialWelfare; // 사회복지시설

    @JsonProperty("JA2103")
    private String corpTypeOrganization; // 기관/단체

    @JsonProperty("JA2201")
    private String corpIndustryManufacturing; // 제조업

    @JsonProperty("JA2202")
    private String corpIndustryAgriculture; // 농업,임업 및 어업

    @JsonProperty("JA2203")
    private String corpIndustryIT; // 정보통신업

    @JsonProperty("JA2299")
    private String corpIndustryEtc; // 기타업종
}
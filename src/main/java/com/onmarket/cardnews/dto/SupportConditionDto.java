package com.onmarket.cardnews.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportConditionDto {
    private Long id; private String serviceId;
    private String genderMale; private String genderFemale;
    private Integer ageStart; private Integer ageEnd;
    private String incomeBracket1; private String incomeBracket2; private String incomeBracket3; private String incomeBracket4; private String incomeBracket5;
    private String jobEmployee; private String jobSeeker;
    private String householdSinglePerson; private String householdMultiChild; private String householdNoHome;
    private String businessProspective; private String businessOperating; private String businessStruggling;
}
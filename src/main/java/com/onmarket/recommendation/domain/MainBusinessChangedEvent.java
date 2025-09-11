package com.onmarket.recommendation.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MainBusinessChangedEvent {
    private final String memberEmail;
    private final Long previousBusinessId;
    private final Long newBusinessId;
    private final BusinessChangeType changeType;

    public enum BusinessChangeType {
        BUSINESS_SWITCH,    // 대표 사업장 변경
        BUSINESS_INFO_UPDATED,  // 대표 사업장 정보 수정
        BUSINESS_DELETED    // 대표 사업장 삭제
    }
}
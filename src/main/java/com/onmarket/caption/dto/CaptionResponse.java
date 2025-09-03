package com.onmarket.caption.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CaptionResponse {
    private List<String> imageUrls;    // 사용된 이미지들
    private List<String> candidates;   // 생성된 캡션 후보
    private String toneApplied;
    private String languageApplied;
    private boolean deleted;           // (파일 업로드의 경우) 모두 삭제 성공 여부
}
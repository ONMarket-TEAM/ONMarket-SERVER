package com.onmarket.caption.dto;

import java.util.List;
import lombok.*;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class CaptionGenerateResponse {
    private String caption; // 생성된 본문
    private List<String> hashtags; // 선택
    private String bestPostTime; // 선택 (예: "오후 7-9시")
    private String impactNote; // 선택
    private boolean sourceDeleted; // S3 삭제 성공 여부
}
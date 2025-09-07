package com.onmarket.post.dto;

import com.onmarket.post.domain.PostType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostListResponse {
    private Long postId;
    private PostType postType;      // 타입 (대출/공공지원금)
    private String companyName;     // 기관명
    private String deadline;        // 마감일 (D-12 형태로 변환 필요)
    private String productName;     // 상품명
    private String summary;         // 요약 정보
}
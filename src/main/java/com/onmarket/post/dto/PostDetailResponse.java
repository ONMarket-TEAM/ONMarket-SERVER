package com.onmarket.post.dto;

import com.onmarket.post.domain.PostType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostDetailResponse {
    private Long postId;
    private String productName;     // 상품 이름
    private String imageUrl;        // 상품 이미지
    private LocalDateTime createdAt; // 게시물 발행일
    private String joinLink;        // 가입하기 버튼 링크
    private String detailContent;   // 상세 내용 (가운데 영역)
}
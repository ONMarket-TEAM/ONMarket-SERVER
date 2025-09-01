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
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private String url;
    private String imageUrl;
}
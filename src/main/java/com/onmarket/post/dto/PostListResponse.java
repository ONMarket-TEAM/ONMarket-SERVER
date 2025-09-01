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
    private String title;
    private PostType postType;
    private String summary;
    private String dDay;
}
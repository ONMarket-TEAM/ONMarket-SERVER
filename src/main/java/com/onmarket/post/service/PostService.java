package com.onmarket.post.service;

import com.onmarket.post.domain.PostType;
import com.onmarket.post.dto.PostDetailResponse;
import com.onmarket.post.dto.PostListResponse;

import java.util.List;

public interface PostService {
    List<PostListResponse> getPostsByType(PostType postType);
    PostDetailResponse getPostById(Long postId);
}

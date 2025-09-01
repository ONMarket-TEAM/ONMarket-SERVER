package com.onmarket.post.controller;

import com.onmarket.common.response.ApiResponse;
import com.onmarket.common.response.ResponseCode;
import com.onmarket.post.domain.PostType;
import com.onmarket.post.dto.PostDetailResponse;
import com.onmarket.post.dto.PostListResponse;
import com.onmarket.post.service.PostService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name="Post API", description = "상품 게시물 관련 API")
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    @GetMapping("/type/{postType}")
    public ApiResponse<List<PostListResponse>> getPostsByType(@PathVariable("postType") PostType postType) {
        List<PostListResponse> postsByType = postService.getPostsByType(postType);
        return ApiResponse.success(ResponseCode.POST_LIST_SUCCESS, postsByType);
    }

    @GetMapping("/{postId}")
    public ApiResponse<PostDetailResponse> getPost(@PathVariable("postId") Long postId) {
        PostDetailResponse post = postService.getPostById(postId);
        return ApiResponse.success(ResponseCode.POST_DETAIL_SUCCESS, post);
    }
}

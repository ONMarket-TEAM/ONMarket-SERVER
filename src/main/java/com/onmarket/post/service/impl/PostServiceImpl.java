package com.onmarket.post.service.impl;

import com.onmarket.common.response.ResponseCode;
import com.onmarket.post.domain.Post;
import com.onmarket.post.domain.PostType;
import com.onmarket.post.dto.PostDetailResponse;
import com.onmarket.post.dto.PostListResponse;
import com.onmarket.post.exception.PostNotFoundException;
import com.onmarket.post.repository.PostRepository;
import com.onmarket.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;

    @Override
    public List<PostListResponse> getPostsByType(PostType postType) {
        List<Post> posts = postRepository.findByPostTypeOrderByCreatedAtDesc(postType);
        return posts.stream().map(this::convertToPostListResponse).collect(Collectors.toList());
    }

    @Override
    public PostDetailResponse getPostById(Long postId) {
        Post post = postRepository.findByPostId(postId).orElseThrow(() -> new PostNotFoundException(ResponseCode.POST_NOT_FOUND));
        return convertToPostDetailResponse(post);
    }

    private PostListResponse convertToPostListResponse(Post post) {
        return PostListResponse.builder()
                .postId(post.getPostId())
                .postType(post.getPostType())
                .title("null")
                .summary(post.getSummary())
                .dDay("null")
                .build();
    }

    private PostDetailResponse convertToPostDetailResponse(Post post) {
        return PostDetailResponse.builder()
                .postId(post.getPostId())
                .title("null") //상품
                .content("null") //gpt
                .url("null") //상품
                .imageUrl("null") //gpt
                .createdAt(post.getCreatedAt())
                .build();
    }
}

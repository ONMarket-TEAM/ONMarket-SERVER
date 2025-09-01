package com.onmarket.post.service.impl;

import com.onmarket.post.domain.Post;
import com.onmarket.post.domain.PostType;
import com.onmarket.post.dto.PostListResponse;
import com.onmarket.post.repository.PostRepository;
import com.onmarket.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
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

    private PostListResponse convertToPostListResponse(Post post) {
        Long productId = 0L;
        if(post.getLoanId() != null) {
            productId = post.getLoanId();
        }else if(post.getPolicyId() != null) {
            productId = post.getPolicyId();
        }
        return PostListResponse.builder()
                .postId(post.getPostId())
                .postType(post.getPostType())
                .title("null")
                .summary(post.getSummary())
                .dDay("null")
                .build();
    }
}

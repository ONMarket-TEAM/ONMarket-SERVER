package com.onmarket.post.service;

import com.onmarket.post.domain.PostType;
import com.onmarket.post.dto.PostDetailResponse;
import com.onmarket.post.dto.PostDetailWithScrapResponse;
import com.onmarket.post.dto.PostListResponse;
import java.util.List;

public interface PostService {
    // 전체 게시물 목록 조회
    List<PostListResponse> getAllPosts();

    // 타입별 게시물 목록 조회
    List<PostListResponse> getPostsByType(PostType postType);

    // 게시물 상세 조회
    PostDetailResponse getPostDetail(Long postId);

    // CreditLoanProduct에서 Post 생성
    void createPostsFromCreditLoanProducts();

    // LoanProduct에서 Post 생성
    void createPostsFromLoanProducts();

    // 스크랩 관련 추가 메서드
    PostDetailWithScrapResponse getPostDetailWithScrap(Long postId, Long memberId);
}

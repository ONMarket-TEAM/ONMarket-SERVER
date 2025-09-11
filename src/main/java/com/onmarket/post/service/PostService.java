package com.onmarket.post.service;

import com.onmarket.post.domain.PostType;
import com.onmarket.post.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PostService {
    // Pageable을 파라미터로 받고, Page<PostListResponse>를 반환하도록 변경
    Page<PostListResponse> getPostsByType(PostType postType, Pageable pageable);

    // Top 5 게시물 조회
    List<PostSingleResponse> getTopScrapedPosts();

    // 게시물 상세 조회
    PostDetailResponse getPostDetail(Long postId);

    // CreditLoanProduct에서 Post 생성
    void createPostsFromCreditLoanProducts();

    // LoanProduct에서 Post 생성
    void createPostsFromLoanProducts();

    // 스크랩 관련 추가 메서드
    PostDetailWithScrapResponse getPostDetailWithScrap(Long postId, String email);

    // SupportProduct에서 Post 생성 (새로 추가)
    void createPostsFromSupportProducts();

    PostSingleResponse getPostById(Long postId);

    //  검색 기능
    Page<PostListResponse> searchPosts(PostType postType, String keyword, String companyName, Pageable pageable);

    void synchronizeModifiedPosts();
}

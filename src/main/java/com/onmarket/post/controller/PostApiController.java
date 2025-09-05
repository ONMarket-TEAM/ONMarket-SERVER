package com.onmarket.post.controller;

import com.onmarket.post.domain.PostType;
import com.onmarket.post.dto.PostDetailWithScrapResponse;
import com.onmarket.post.dto.PostListResponse;
import com.onmarket.post.service.PostService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Post API", description = "상품 게시물 관련 API")
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostApiController {

    private final PostService postService;

    /**
     * 전체 게시물 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<PostListResponse>> getAllPosts() {
        List<PostListResponse> posts = postService.getAllPosts();
        return ResponseEntity.ok(posts);
    }

    /**
     * 타입별 게시물 목록 조회
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<PostListResponse>> getPostsByType(@PathVariable PostType type) {
        List<PostListResponse> posts = postService.getPostsByType(type);
        return ResponseEntity.ok(posts);
    }

    /**
     * 게시물 상세 조회 (스크랩 정보 포함)
     */
    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailWithScrapResponse> getPostDetail(
            @PathVariable Long postId,
            @RequestParam(required = false) Long memberId) {

        PostDetailWithScrapResponse response = postService.getPostDetailWithScrap(postId, memberId);
        return ResponseEntity.ok(response);
    }

    /**
     * CreditLoanProduct 데이터 동기화 (관리자용)
     */
    @PostMapping("/sync/credit-loans")
    public ResponseEntity<String> syncCreditLoanPosts() {
        postService.createPostsFromCreditLoanProducts();
        return ResponseEntity.ok("신용대출 상품 게시물 동기화 완료");
    }

    /**
     * LoanProduct 데이터 동기화 (관리자용)
     */
    @PostMapping("/sync/general-loans")
    public ResponseEntity<String> syncGeneralLoanPosts() {
        postService.createPostsFromLoanProducts();
        return ResponseEntity.ok("일반대출 상품 게시물 동기화 완료");
    }
}
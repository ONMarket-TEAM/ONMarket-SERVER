package com.onmarket.post.controller;

import com.onmarket.common.response.ApiResponse;
import com.onmarket.common.response.ResponseCode;
import com.onmarket.member.exception.LoginException;
import com.onmarket.oauth.jwt.JwtTokenProvider;
import com.onmarket.post.domain.PostType;
import com.onmarket.post.dto.PostDetailWithScrapResponse;
import com.onmarket.post.dto.PostListResponse;
import com.onmarket.post.service.PostService;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Post API", description = "상품 게시물 관련 API")
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class PostApiController {

    private final PostService postService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 타입별 게시물 목록 조회
     */
    @GetMapping("/type/{type}")
    public ApiResponse<List<PostListResponse>> getPostsByType(@PathVariable PostType type) {
        List<PostListResponse> posts = postService.getPostsByType(type);
        return ApiResponse.success(ResponseCode.POST_LIST_SUCCESS, posts);
    }

    /**
     * 게시물 상세 조회 (스크랩 정보 포함)
     */
    @GetMapping("/{postId}")
    public ApiResponse<PostDetailWithScrapResponse> getPostDetail(HttpServletRequest request,  @PathVariable Long postId) {
        String email = extractEmailFromToken(request);
        PostDetailWithScrapResponse response = postService.getPostDetailWithScrap(postId, email);
        return ApiResponse.success(ResponseCode.POST_DETAIL_SUCCESS, response);
    }

    @GetMapping("/recommendation/{postId}")
    public ApiResponse<PostListResponse> getPostById(@PathVariable Long postId) {
        PostListResponse response = postService.getPostById(postId);
        return ApiResponse.success(ResponseCode.POST_LIST_SUCCESS, response);
    }

    /**
     * CreditLoanProduct 데이터 동기화 (관리자용)
     */
    @PostMapping("/sync/credit-loans")
    public ApiResponse<String> syncCreditLoanPosts() {
        postService.createPostsFromCreditLoanProducts();
        return ApiResponse.success(ResponseCode.POST_CREDIT_LOAN_CREATE_SUCCESS);
    }

    /**
     * LoanProduct 데이터 동기화 (관리자용)
     */
    @PostMapping("/sync/general-loans")
    public ApiResponse<String> syncGeneralLoanPosts() {
        postService.createPostsFromLoanProducts();
        return ApiResponse.success(ResponseCode.POST_LOAN_CREATE_SUCCESS);
    }

    /**
     * SupportProduct 데이터 동기화 (관리자용) - 새로 추가
     */
    @PostMapping("/sync/support-products")
    public ApiResponse<String> syncSupportPosts() {
        postService.createPostsFromSupportProducts();
        return ApiResponse.success(ResponseCode.POST_SUPPORT_CREATE_SUCCESS);
    }

    private String extractEmailFromToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new LoginException(ResponseCode.AUTH_TOKEN_NOT_FOUND);
        }

        String token = header.substring(7);

        if (!jwtTokenProvider.validateToken(token)) {
            throw new LoginException(ResponseCode.AUTH_TOKEN_INVALID);
        }

        return jwtTokenProvider.getEmail(token);
    }
}
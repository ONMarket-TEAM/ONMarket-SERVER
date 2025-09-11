package com.onmarket.post.controller;

import com.onmarket.common.response.ApiResponse;
import com.onmarket.common.response.ResponseCode;
import com.onmarket.member.exception.LoginException;
import com.onmarket.oauth.jwt.JwtTokenProvider;
import com.onmarket.post.domain.PostType;
import com.onmarket.post.dto.PostDetailWithScrapResponse;
import com.onmarket.post.dto.PostListResponse;
import com.onmarket.post.dto.PostSingleResponse;
import com.onmarket.post.service.PostService;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.List;


@Slf4j
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
    public ApiResponse<Page<PostListResponse>> getPostsByType(@PathVariable PostType type, Pageable pageable) {
        Page<PostListResponse> postsPage = postService.getPostsByType(type, pageable);
        return ApiResponse.success(ResponseCode.POST_LIST_SUCCESS, postsPage);
    }

    /**
     * 스크랩 수 상위 5개 게시물 조회
     */
    @Operation(summary = "TOP 5 게시물 조회", description = "스크랩 수가 가장 많은 상위 5개 게시물 조회")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/top-scraped")
    public ApiResponse<List<PostListResponse>> getTopScrapedPosts() {
        log.info("인기 상품 Top 5 조회 요청");

        List<PostListResponse> topPosts = postService.getTopScrapedPosts();

        log.info("인기 상품 Top 5 조회 완료 - {}개 반환", topPosts.size());

        return ApiResponse.success(ResponseCode.POST_LIST_SUCCESS, topPosts);
    }

    /**
     * 상품 검색 API
     */
    @Operation(summary = "상품 검색", description = "타입별 상품을 키워드로 검색합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/type/{type}/search")
    public ApiResponse<Page<PostListResponse>> searchPosts(
            @Parameter(description = "상품 타입", example = "LOAN")
            @PathVariable PostType type,

            @Parameter(description = "검색 키워드 (상품명, 회사명에서 검색)", example = "신한")
            @RequestParam(value = "keyword", required = false) String keyword,

            @Parameter(description = "회사명 필터", example = "신한은행")
            @RequestParam(value = "company", required = false) String companyName,

            Pageable pageable,
            HttpServletRequest request) { // 🔥 요청 정보 로깅을 위해 추가

        // 🔥 디버깅을 위한 로깅 추가
        log.info("검색 요청 - URL: {}, 타입: {}, 키워드: '{}', 회사명: '{}', 페이지: {}",
                request.getRequestURL() + "?" + request.getQueryString(),
                type, keyword, companyName, pageable.getPageNumber());

        try {
            Page<PostListResponse> searchResults = postService.searchPosts(
                    type, keyword, companyName, pageable);

            log.info("검색 결과 - 총 {}개, 현재 페이지 {}개",
                    searchResults.getTotalElements(), searchResults.getNumberOfElements());

            return ApiResponse.success(ResponseCode.POST_LIST_SUCCESS, searchResults);

        } catch (Exception e) {
            log.error("검색 중 오류 발생: ", e);
            throw e;
        }
    }

    /**
     * 게시물 상세 조회 (스크랩 정보 포함)
     */
    @GetMapping("/{postId}")
    public ApiResponse<PostDetailWithScrapResponse> getPostDetail(
            HttpServletRequest request,
            @PathVariable Long postId) {
        String email = extractEmailFromToken(request);
        PostDetailWithScrapResponse response = postService.getPostDetailWithScrap(postId, email);
        return ApiResponse.success(ResponseCode.POST_DETAIL_SUCCESS, response);
    }

    /**
     * 추천용 단일 게시물 조회
     */
    @GetMapping("/recommendation/{postId}")
    public ApiResponse<PostSingleResponse> getPostById(@PathVariable Long postId) {
        PostSingleResponse response = postService.getPostById(postId);
        return ApiResponse.success(ResponseCode.POST_LIST_SUCCESS, response);
    }

    /**
     * 신용대출 상품 데이터 동기화 (관리자용)
     */
    @Operation(summary = "신용대출 상품 동기화", description = "CreditLoanProduct 데이터를 Post로 동기화합니다")
    @PostMapping("/sync/credit-loans")
    public ApiResponse<String> syncCreditLoanPosts() {
        postService.createPostsFromCreditLoanProducts();
        return ApiResponse.success(ResponseCode.POST_CREDIT_LOAN_CREATE_SUCCESS);
    }

    /**
     * 일반대출 상품 데이터 동기화 (관리자용)
     */
    @Operation(summary = "일반대출 상품 동기화", description = "LoanProduct 데이터를 Post로 동기화합니다")
    @PostMapping("/sync/general-loans")
    public ApiResponse<String> syncGeneralLoanPosts() {
        postService.createPostsFromLoanProducts();
        return ApiResponse.success(ResponseCode.POST_LOAN_CREATE_SUCCESS);
    }

    /**
     * 공공지원금 상품 데이터 동기화 (관리자용)
     */
    @Operation(summary = "공공지원금 상품 동기화", description = "SupportProduct 데이터를 Post로 동기화합니다")
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
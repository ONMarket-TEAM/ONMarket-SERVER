package com.onmarket.comment.controller;

import com.onmarket.comment.dto.CommentCreateRequest;
import com.onmarket.comment.dto.CommentListResponse;
import com.onmarket.comment.dto.CommentResponse;
import com.onmarket.comment.dto.CommentUpdateRequest;
import com.onmarket.comment.service.CommentService;
import com.onmarket.common.response.ApiResponse;
import com.onmarket.common.response.ResponseCode;
import com.onmarket.member.exception.LoginException;
import com.onmarket.oauth.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Comment API", description = "댓글 관련 API")
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class CommentApiController {

    private final CommentService commentService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 댓글 작성 (일반 댓글 또는 대댓글)
     */
    @Operation(summary = "댓글 작성", description = "게시물에 댓글 또는 대댓글을 작성합니다.")
    @PostMapping
    public ApiResponse<CommentResponse> createComment(
            HttpServletRequest request,
            @Valid @RequestBody CommentCreateRequest commentRequest) {

        UserInfo userInfo = extractUserInfoFromToken(request);
        CommentResponse response = commentService.createComment(
                commentRequest, userInfo.email(), userInfo.name());

        return ApiResponse.success(ResponseCode.COMMENT_CREATE_SUCCESS, response);
    }

    /**
     * 댓글 수정
     */
    @Operation(summary = "댓글 수정", description = "작성한 댓글을 수정합니다.")
    @PutMapping("/{commentId}")
    public ApiResponse<CommentResponse> updateComment(
            HttpServletRequest request,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest updateRequest) {

        String userEmail = extractEmailFromToken(request);
        CommentResponse response = commentService.updateComment(commentId, updateRequest, userEmail);

        return ApiResponse.success(ResponseCode.COMMENT_UPDATE_SUCCESS, response);
    }

    /**
     * 댓글 삭제
     */
    @Operation(summary = "댓글 삭제", description = "작성한 댓글을 삭제합니다.")
    @DeleteMapping("/{commentId}")
    public ApiResponse<String> deleteComment(
            HttpServletRequest request,
            @PathVariable Long commentId) {

        String userEmail = extractEmailFromToken(request);
        commentService.deleteComment(commentId, userEmail);

        return ApiResponse.success(ResponseCode.COMMENT_DELETE_SUCCESS);
    }

    /**
     * 게시물별 댓글 목록 조회
     */
    @Operation(summary = "댓글 목록 조회", description = "특정 게시물의 댓글 목록을 조회합니다.")
    @GetMapping("/post/{postId}")
    public ApiResponse<CommentListResponse> getCommentsByPostId(
            HttpServletRequest request,
            @PathVariable Long postId) {

        String userEmail = extractEmailFromToken(request);
        CommentListResponse response = commentService.getCommentsByPostId(postId, userEmail);

        return ApiResponse.success(ResponseCode.COMMENT_LIST_SUCCESS, response);
    }

    /**
     * 댓글 상세 조회
     */
    @Operation(summary = "댓글 상세 조회", description = "특정 댓글의 상세 정보를 조회합니다.")
    @GetMapping("/{commentId}")
    public ApiResponse<CommentResponse> getComment(
            HttpServletRequest request,
            @PathVariable Long commentId) {

        String userEmail = extractEmailFromToken(request);
        CommentResponse response = commentService.getComment(commentId, userEmail);

        return ApiResponse.success(ResponseCode.COMMENT_DETAIL_SUCCESS, response);
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

    private UserInfo extractUserInfoFromToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new LoginException(ResponseCode.AUTH_TOKEN_NOT_FOUND);
        }

        String token = header.substring(7);

        if (!jwtTokenProvider.validateToken(token)) {
            throw new LoginException(ResponseCode.AUTH_TOKEN_INVALID);
        }

        String email = jwtTokenProvider.getEmail(token);
        // 만약 JwtTokenProvider에 getName 메서드가 없다면 아래처럼 임시로 처리
        String name = getUserNameFromEmailOrToken(email, token);

        return new UserInfo(email, name);
    }

    // JwtTokenProvider에 getName이 없을 경우 임시 처리 메서드
    private String getUserNameFromEmailOrToken(String email, String token) {
        // 1. JwtTokenProvider에 getName 메서드가 있다면 이 메서드 삭제하고 jwtTokenProvider.getName(token) 사용
        // 2. 없다면 이메일에서 @앞부분을 이름으로 사용하거나 별도 User 서비스에서 조회
        return email.split("@")[0]; // 임시: 이메일 @앞부분을 이름으로 사용
    }

    // 사용자 정보를 담는 레코드 클래스
    private record UserInfo(String email, String name) {}
}

package com.onmarket.comment.service;

import com.onmarket.comment.dto.CommentCreateRequest;
import com.onmarket.comment.dto.CommentListResponse;
import com.onmarket.comment.dto.CommentResponse;
import com.onmarket.comment.dto.CommentUpdateRequest;

public interface CommentService {

    /**
     * 댓글 작성 (일반 댓글 또는 대댓글)
     */
    CommentResponse createComment(CommentCreateRequest request, String userEmail, String author);

    /**
     * 댓글 수정
     */
    CommentResponse updateComment(Long commentId, CommentUpdateRequest request, String userEmail);

    /**
     * 댓글 삭제 (논리 삭제)
     */
    void deleteComment(Long commentId, String userEmail);

    /**
     * 게시물별 댓글 목록 조회
     */
    CommentListResponse getCommentsByPostId(Long postId, String currentUserEmail);

    /**
     * 댓글 상세 조회
     */
    CommentResponse getComment(Long commentId, String currentUserEmail);
}
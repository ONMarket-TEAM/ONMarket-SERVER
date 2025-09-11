package com.onmarket.comment.dto;

import com.onmarket.comment.domain.Comment;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class CommentResponse {
    private Long commentId;
    private Long postId;
    private String author;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isDeleted;
    private boolean isOwner; // 현재 사용자가 작성한 댓글인지
    private Long parentCommentId; // 부모 댓글 ID (대댓글인 경우)
    private List<CommentResponse> replies; // 대댓글 목록
    private int replyCount; // 대댓글 개수
    private Integer rating; // 별점 (1~5점, null이면 별점 없음)

    public static CommentResponse from(Comment comment, String currentUserEmail, String nickname) {
        List<CommentResponse> replies = comment.getReplies().stream()
                .filter(reply -> !reply.getIsDeleted()) // 삭제되지 않은 대댓글만
                .map(reply -> CommentResponse.from(reply, currentUserEmail, nickname))
                .collect(Collectors.toList());

        return CommentResponse.builder()
                .commentId(comment.getCommentId())
                .postId(comment.getPostId())
                .author(nickname)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .isDeleted(comment.getIsDeleted())
                .isOwner(comment.isOwner(currentUserEmail))
                .parentCommentId(comment.getParentComment() != null ?
                        comment.getParentComment().getCommentId() : null)
                .replies(replies)
                .replyCount(replies.size())
                .rating(comment.getRating()) // 별점 추가
                .build();
    }
}
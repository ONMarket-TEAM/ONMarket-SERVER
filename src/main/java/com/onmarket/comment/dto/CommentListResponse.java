package com.onmarket.comment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CommentListResponse {
    private Long postId;
    private int totalCount; // 전체 댓글 수 (대댓글 포함)
    private int commentCount; // 부모 댓글 수
    private List<CommentResponse> comments; // 댓글 목록 (대댓글 포함)

    public static CommentListResponse of(Long postId, List<CommentResponse> comments) {
        int totalCount = comments.stream()
                .mapToInt(comment -> 1 + comment.getReplyCount())
                .sum();

        return CommentListResponse.builder()
                .postId(postId)
                .totalCount(totalCount)
                .commentCount(comments.size())
                .comments(comments)
                .build();
    }
}

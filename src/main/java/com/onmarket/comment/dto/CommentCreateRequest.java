package com.onmarket.comment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentCreateRequest {

    @NotNull(message = "게시물 ID는 필수입니다.")
    private Long postId;

    @NotBlank(message = "댓글 내용은 필수입니다.")
    private String content;

    private Long parentCommentId;

    @Min(value = 1, message = "별점은 1점 이상이어야 합니다.")
    @Max(value = 5, message = "별점은 5점 이하여야 합니다.")
    private Integer rating;
}
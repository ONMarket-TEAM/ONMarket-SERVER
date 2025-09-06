package com.onmarket.comment.exception;

import com.onmarket.common.exception.BaseException;
import com.onmarket.common.response.ResponseCode;

public class CommentInvalidRatingException extends BaseException {
    public CommentInvalidRatingException() {
        super(ResponseCode.COMMENT_INVALID_RATING);
    }
}
package com.onmarket.comment.exception;

import com.onmarket.common.exception.BaseException;
import com.onmarket.common.response.ResponseCode;

public class CommentReplyRatingNotAllowedException extends BaseException {
    public CommentReplyRatingNotAllowedException() {
        super(ResponseCode.COMMENT_REPLY_RATING_NOT_ALLOWED);
    }
}
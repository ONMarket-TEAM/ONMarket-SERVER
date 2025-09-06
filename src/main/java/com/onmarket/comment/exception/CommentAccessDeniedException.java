package com.onmarket.comment.exception;

import com.onmarket.common.exception.BaseException;
import com.onmarket.common.response.ResponseCode;

public class CommentAccessDeniedException extends BaseException {
    public CommentAccessDeniedException() {
        super(ResponseCode.COMMENT_ACCESS_DENIED);
    }
}

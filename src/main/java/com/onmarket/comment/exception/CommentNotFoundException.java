package com.onmarket.comment.exception;

import com.onmarket.common.exception.BaseException;
import com.onmarket.common.response.ResponseCode;

public class CommentNotFoundException extends BaseException {
    public CommentNotFoundException() {
        super(ResponseCode.COMMENT_NOT_FOUND);
    }
}

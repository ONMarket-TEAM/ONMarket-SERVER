package com.onmarket.comment.exception;

import com.onmarket.common.exception.BaseException;
import com.onmarket.common.response.ResponseCode;

public class ParentCommentNotFoundException extends BaseException {
    public ParentCommentNotFoundException() {
        super(ResponseCode.PARENT_COMMENT_NOT_FOUND);
    }
}

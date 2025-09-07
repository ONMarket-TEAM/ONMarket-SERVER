package com.onmarket.comment.exception;

import com.onmarket.common.exception.BaseException;
import com.onmarket.common.response.ResponseCode;

public class CommentReplyDepthExceededException extends BaseException {
    public CommentReplyDepthExceededException() {
        super(ResponseCode.COMMENT_REPLY_DEPTH_EXCEEDED);
    }
}
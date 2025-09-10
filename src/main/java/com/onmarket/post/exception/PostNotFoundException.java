package com.onmarket.post.exception;

import com.onmarket.common.exception.BaseException;
import com.onmarket.common.response.ResponseCode;

public class PostNotFoundException extends BaseException {
    public PostNotFoundException() {
        super(ResponseCode.POST_NOT_FOUND);
    }
}

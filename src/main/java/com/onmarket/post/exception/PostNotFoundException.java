package com.onmarket.post.exception;

import com.onmarket.common.exception.BaseException;
import com.onmarket.common.response.ResponseCode;

public class PostNotFoundException extends BaseException {
    public PostNotFoundException(ResponseCode responseCode) {
        super(responseCode);
    }
}

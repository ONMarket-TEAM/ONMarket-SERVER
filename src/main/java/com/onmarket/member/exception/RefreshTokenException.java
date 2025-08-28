package com.onmarket.member.exception;

import com.onmarket.common.exception.BaseException;
import com.onmarket.common.response.ResponseCode;

public class RefreshTokenException extends BaseException {
    public RefreshTokenException(ResponseCode responseCode) {
        super(responseCode);
    }
}

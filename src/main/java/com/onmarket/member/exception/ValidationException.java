package com.onmarket.member.exception;

import com.onmarket.common.exception.BaseException;
import com.onmarket.response.ResponseCode;

public class ValidationException extends BaseException {
    public ValidationException(ResponseCode responseCode) {
        super(responseCode);
    }
}

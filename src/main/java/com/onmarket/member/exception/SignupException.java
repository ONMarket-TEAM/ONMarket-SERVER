package com.onmarket.member.exception;

import com.onmarket.common.exception.BaseException;
import com.onmarket.response.ResponseCode;

public class SignupException extends BaseException {
    public SignupException(ResponseCode responseCode) {
        super(responseCode);
    }
}

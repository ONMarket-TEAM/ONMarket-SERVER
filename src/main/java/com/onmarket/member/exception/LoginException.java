package com.onmarket.member.exception;

import com.onmarket.common.exception.BaseException;
import com.onmarket.response.ResponseCode;

public class LoginException extends BaseException {
    public LoginException(ResponseCode responseCode) {
        super(responseCode);
    }
}

package com.onmarket.member.exception;

import com.onmarket.common.exception.BaseException;
import com.onmarket.common.response.ResponseCode;


public class SmsVerifyFailedException extends BaseException {
    public SmsVerifyFailedException(ResponseCode responseCode) {
        super(responseCode);
    }
}

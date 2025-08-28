package com.onmarket.member.exception;

import com.onmarket.common.exception.BaseException;
import com.onmarket.common.response.ResponseCode;

public class SmsSendFailException extends BaseException {
    public SmsSendFailException(ResponseCode responseCode) {
        super(responseCode);
    }
}
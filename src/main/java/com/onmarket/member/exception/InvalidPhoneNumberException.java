package com.onmarket.member.exception;

import com.onmarket.common.exception.BaseException;
import com.onmarket.common.response.ResponseCode;

public class InvalidPhoneNumberException extends BaseException {
    public InvalidPhoneNumberException() {
        super(ResponseCode.INVALID_PHONE_NUMBER);
    }
}
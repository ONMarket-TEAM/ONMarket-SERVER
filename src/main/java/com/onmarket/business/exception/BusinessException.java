package com.onmarket.business.exception;

import com.onmarket.common.exception.BaseException;
import com.onmarket.common.response.ResponseCode;

public class BusinessException extends BaseException {
    public BusinessException(ResponseCode responseCode) {
        super(responseCode);
    }
}

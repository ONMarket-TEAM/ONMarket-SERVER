package com.onmarket.member.exception;

import com.onmarket.common.exception.BaseException;
import com.onmarket.common.response.ResponseCode;

public class MemberNotFountException extends BaseException {
    public MemberNotFountException() {
        super(ResponseCode.MEMBER_NOT_FOUND);
    }
}
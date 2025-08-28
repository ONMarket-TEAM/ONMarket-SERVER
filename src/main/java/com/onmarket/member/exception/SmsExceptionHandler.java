package com.onmarket.member.exception;

import com.onmarket.member.exception.InvalidPhoneNumberException;
import com.onmarket.common.exception.ErrorResponse;
import com.onmarket.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class SmsExceptionHandler {

    @ExceptionHandler(InvalidPhoneNumberException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleInvalidPhoneNumberException(
            InvalidPhoneNumberException exception,
            HttpServletRequest request) {
        return buildErrorResponse(exception, request);
    }

    @ExceptionHandler(SmsSendFailException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleSmsSendFailException(
            SmsSendFailException exception,
            HttpServletRequest request) {
        return buildErrorResponse(exception, request);
    }

    @ExceptionHandler(SmsVerifyFailedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleSmsVerifyFailedException(
            SmsVerifyFailedException exception,
            HttpServletRequest request) {
        return buildErrorResponse(exception, request);
    }

    private ResponseEntity<ApiResponse<ErrorResponse>> buildErrorResponse(
            com.onmarket.common.exception.BaseException exception,
            HttpServletRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                exception.getMessage(),
                exception.getResponseCode().name(),
                request.getRequestURI()
        );

        ApiResponse<ErrorResponse> apiResponse =
                ApiResponse.fail(exception.getResponseCode(), errorResponse);

        return new ResponseEntity<>(apiResponse, exception.getResponseCode().getHttpStatus());
    }
}


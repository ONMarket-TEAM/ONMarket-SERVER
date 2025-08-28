package com.onmarket.business.exception;

import com.onmarket.common.exception.ErrorResponse;
import com.onmarket.response.ApiResponse;
import com.onmarket.response.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice(basePackages = "com.onmarket.business")
public class BusinessExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request) {
        return buildErrorResponse(exception.getResponseCode(), request);
    }

    /**
     * 예상치 못한 예외 처리 (fallback)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                exception.getMessage(),
                ResponseCode.SERVER_ERROR.name(),
                request.getRequestURI()
        );

        ApiResponse<ErrorResponse> apiResponse =
                ApiResponse.fail(ResponseCode.SERVER_ERROR, errorResponse);

        return new ResponseEntity<>(apiResponse, ResponseCode.SERVER_ERROR.getHttpStatus());
    }

    /**
     * 공통 에러 응답 빌더
     */
    private ResponseEntity<ApiResponse<ErrorResponse>> buildErrorResponse(
            ResponseCode responseCode,
            HttpServletRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                responseCode.getMessage(),
                responseCode.name(),
                request.getRequestURI()
        );

        ApiResponse<ErrorResponse> apiResponse = ApiResponse.fail(responseCode, errorResponse);
        return new ResponseEntity<>(apiResponse, responseCode.getHttpStatus());
    }
}

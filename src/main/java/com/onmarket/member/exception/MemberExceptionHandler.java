package com.onmarket.member.exception;

import com.onmarket.common.exception.ErrorResponse;
import com.onmarket.response.ApiResponse;
import com.onmarket.response.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice(basePackages = "com.onmarket.member") // member 패키지 전용
public class MemberExceptionHandler {

    @ExceptionHandler(LoginException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleLoginException(
            LoginException exception,
            HttpServletRequest request) {
        return buildErrorResponse(exception.getResponseCode(), request);
    }

    @ExceptionHandler(SignupException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleSignupException(
            SignupException exception,
            HttpServletRequest request) {
        return buildErrorResponse(exception.getResponseCode(), request);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidationException(
            ValidationException exception,
            HttpServletRequest request) {
        return buildErrorResponse(exception.getResponseCode(), request);
    }

    @ExceptionHandler(RefreshTokenException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleRefreshTokenException(
            RefreshTokenException exception,
            HttpServletRequest request) {
        return buildErrorResponse(exception.getResponseCode(), request);
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

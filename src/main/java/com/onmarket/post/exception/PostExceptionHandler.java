package com.onmarket.post.exception;

import com.onmarket.common.exception.ErrorResponse;
import com.onmarket.common.response.ApiResponse;
import com.onmarket.member.exception.LoginException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice(basePackages = "com.onmarket.post")
public class PostExceptionHandler {


    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handlePostNotFoundException(
            PostNotFoundException exception,
            HttpServletRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                exception.getMessage(),
                exception.getResponseCode().name(),
                request.getRequestURI()
        );

        ApiResponse<ErrorResponse> apiResponse = ApiResponse.fail(exception.getResponseCode(), errorResponse);

        return new ResponseEntity<>(apiResponse, exception.getResponseCode().getHttpStatus());
    }
}

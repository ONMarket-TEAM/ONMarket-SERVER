package com.onmarket.comment.exception;

import com.onmarket.common.exception.ErrorResponse;
import com.onmarket.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice(basePackages = "com.onmarket.comment")
public class CommentExceptionHandler {


    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleCommentNotFoundException(
            CommentNotFoundException exception,
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
    @ExceptionHandler(CommentAccessDeniedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleCommentAccessDeniedException(
            CommentAccessDeniedException exception,
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
    @ExceptionHandler(ParentCommentNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleParentCommentNotFoundException(
            ParentCommentNotFoundException exception,
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

    @ExceptionHandler(CommentInvalidRatingException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleCommentInvalidRatingException(
            CommentInvalidRatingException exception,
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

    @ExceptionHandler(CommentReplyRatingNotAllowedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleCommentReplyRatingNotAllowedException(
            CommentReplyRatingNotAllowedException exception,
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

    @ExceptionHandler(CommentReplyDepthExceededException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleCommentReplyDepthExceededException(
            CommentReplyDepthExceededException exception,
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

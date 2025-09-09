package com.onmarket.notification.exception;

import com.onmarket.common.exception.ErrorResponse;
import com.onmarket.common.response.ApiResponse;
import com.onmarket.common.response.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice(basePackages = "com.onmarket.notification")
@Slf4j
public class NotificationExceptionHandler {

    /**
     * 알림을 찾을 수 없는 경우
     */
    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleNotificationNotFound(
            NotificationNotFoundException exception,
            HttpServletRequest request) {
        log.warn("알림을 찾을 수 없음: {}", exception.getMessage());
        return buildErrorResponse(ResponseCode.NOTIFICATION_NOT_FOUND, request);
    }

    /**
     * 알림 접근 권한 없음
     */
    @ExceptionHandler(InvalidNotificationAccessException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleInvalidAccess(
            InvalidNotificationAccessException exception,
            HttpServletRequest request) {
        log.warn("알림 접근 권한 없음: {}", exception.getMessage());
        return buildErrorResponse(ResponseCode.NOTIFICATION_ACCESS_DENIED, request);
    }

    /**
     * 구독 정보 관련 에러
     */
    @ExceptionHandler(NotificationSubscriptionNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleSubscriptionNotFound(
            NotificationSubscriptionNotFoundException exception,
            HttpServletRequest request) {
        log.warn("구독 정보 문제: {}", exception.getMessage());
        return buildErrorResponse(ResponseCode.WEB_PUSH_INVALID_SUBSCRIPTION, request);
    }

    /**
     * 푸시 알림 발송 실패
     */
    @ExceptionHandler(PushNotificationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handlePushNotificationError(
            PushNotificationException exception,
            HttpServletRequest request) {
        log.error("푸시 알림 발송 실패: {}", exception.getMessage(), exception);
        return buildErrorResponse(ResponseCode.WEB_PUSH_SEND_FAILED, request);
    }

    /**
     * 잘못된 요청 데이터 (Validation 실패)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidationError(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        String errorMessage = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("잘못된 요청입니다.");

        log.warn("요청 데이터 검증 실패: {}", errorMessage);

        // Validation 에러는 별도 처리 (custom message 사용)
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                errorMessage,
                ResponseCode.INVALID_REQUEST_PARAM.name(),
                request.getRequestURI()
        );

        ApiResponse<ErrorResponse> apiResponse = ApiResponse.fail(ResponseCode.INVALID_REQUEST_PARAM, errorResponse);
        return new ResponseEntity<>(apiResponse, ResponseCode.INVALID_REQUEST_PARAM.getHttpStatus());
    }

    /**
     * 일반적인 IllegalArgument 예외
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request) {
        log.warn("잘못된 요청: {}", exception.getMessage());
        return buildErrorResponse(ResponseCode.INVALID_REQUEST_PARAM, request);
    }

    /**
     * 예상하지 못한 서버 에러
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleGenericError(
            Exception exception,
            HttpServletRequest request) {
        log.error("예상하지 못한 서버 에러: {}", exception.getMessage(), exception);
        return buildErrorResponse(ResponseCode.SERVER_ERROR, request);
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

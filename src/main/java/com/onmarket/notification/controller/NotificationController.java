package com.onmarket.notification.controller;

import com.onmarket.common.response.ApiResponse;
import com.onmarket.common.response.ResponseCode;
import com.onmarket.notification.dto.*;
import com.onmarket.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "알림", description = "알림 관리 API")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "알림 목록 조회", description = "사용자의 알림 목록을 페이징하여 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiResponse<Page<NotificationListResponse>> getNotifications(
            @AuthenticationPrincipal String email,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationListResponse> notifications = notificationService.getNotifications(email, pageable);

        return ApiResponse.success(ResponseCode.NOTIFICATION_LIST_SUCCESS, notifications);
    }

    @GetMapping("/summary")
    @Operation(summary = "알림 요약 조회", description = "읽지 않은 알림 개수와 구독 상태를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiResponse<NotificationSummaryResponse> getNotificationSummary(
            @AuthenticationPrincipal String email) {

        NotificationSummaryResponse summary = notificationService.getNotificationSummary(email);
        return ApiResponse.success(ResponseCode.NOTIFICATION_UNREAD_COUNT_SUCCESS, summary);
    }

    @PatchMapping("/read")
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음으로 표시합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "알림 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ApiResponse<Void> markNotificationAsRead(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody NotificationMarkReadRequest request) {

        notificationService.markAsRead(email, request.getNotificationId());
        return ApiResponse.success(ResponseCode.NOTIFICATION_READ_SUCCESS);
    }

    @PatchMapping("/read-all")
    @Operation(summary = "모든 알림 읽음 처리", description = "사용자의 모든 미읽음 알림을 읽음으로 표시합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiResponse<NotificationMarkAllReadResponse> markAllNotificationsAsRead(
            @AuthenticationPrincipal String email) {

        int markedCount = notificationService.markAllAsRead(email);
        NotificationMarkAllReadResponse response = NotificationMarkAllReadResponse.of(markedCount);

        return ApiResponse.success(ResponseCode.NOTIFICATION_READ_ALL_SUCCESS, response);
    }

    @PostMapping("/subscribe")
    @Operation(summary = "웹 푸시 구독", description = "웹 푸시 알림을 구독합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "구독 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 구독 정보"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiResponse<NotificationSubscriptionResponse> subscribe(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody NotificationSubscriptionRequest request) {

        notificationService.subscribe(email, request);
        NotificationSubscriptionResponse response = NotificationSubscriptionResponse.of(true);

        return ApiResponse.success(ResponseCode.WEB_PUSH_SUBSCRIBE_SUCCESS, response);
    }

    @DeleteMapping("/unsubscribe")
    @Operation(summary = "웹 푸시 구독 해제", description = "웹 푸시 알림 구독을 해제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "구독 해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiResponse<NotificationSubscriptionResponse> unsubscribe(
            @AuthenticationPrincipal String email) {

        notificationService.unsubscribe(email);
        NotificationSubscriptionResponse response = NotificationSubscriptionResponse.of(false);

        return ApiResponse.success(ResponseCode.WEB_PUSH_UNSUBSCRIBE_SUCCESS, response);
    }

    @GetMapping("/subscription-status")
    @Operation(summary = "웹 푸시 구독 상태 조회", description = "현재 웹 푸시 구독 상태를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiResponse<NotificationSubscriptionResponse> getSubscriptionStatus(
            @AuthenticationPrincipal String email) {

        boolean isSubscribed = notificationService.isSubscribed(email);
        NotificationSubscriptionResponse response = NotificationSubscriptionResponse.of(isSubscribed);

        return ApiResponse.success(ResponseCode.WEB_PUSH_STATUS_SUCCESS, response);
    }

    // 관리자용 API (스케줄러에서 호출하거나 수동 실행용)
    @PostMapping("/admin/create-deadline-notifications")
    @Operation(summary = "마감일 알림 생성", description = "모든 스크랩된 상품의 마감일 알림을 생성합니다. (관리자용)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "알림 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ApiResponse<Void> createDeadlineNotifications() {
        log.info("관리자에 의한 마감일 알림 생성 요청");

        notificationService.createDeadlineNotifications();

        return ApiResponse.success(ResponseCode.NOTIFICATION_CREATE_DDAY_SUCCESS);
    }
}

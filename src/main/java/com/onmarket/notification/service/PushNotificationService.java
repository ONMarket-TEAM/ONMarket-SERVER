package com.onmarket.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onmarket.notification.domain.NotificationSubscription;
import com.zerodeplibs.webpush.PushSubscription;
import com.zerodeplibs.webpush.VAPIDKeyPair;
import com.zerodeplibs.webpush.httpclient.StandardHttpClientRequestPreparer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final VAPIDKeyPair vapidKeyPair;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${vapid.subject}")
    private String vapidSubject;

    public void sendPushNotification(NotificationSubscription subscription, String title, String message)
            throws Exception {

        // 1) 우리 도메인 → zerodeplibs PushSubscription 로 어댑터 변환
        Map<String, Object> subscriptionJson = Map.of(
                "endpoint", subscription.getEndpoint(),
                "keys", Map.of(
                        "p256dh", subscription.getP256dhKey(),
                        "auth",   subscription.getAuthKey()
                )
        );
        PushSubscription pushSubscription = objectMapper.convertValue(subscriptionJson, PushSubscription.class);

        // 2) Payload 생성
        Map<String, Object> payload = Map.of(
                "title", title,
                "body",  message,
                "icon",  "/icons/logo.png",
                "url",   "/"
        );
        byte[] messageBytes = objectMapper.writeValueAsBytes(payload);

        // 3) 요청 생성/전송
        HttpRequest request = StandardHttpClientRequestPreparer.getBuilder()
                .pushSubscription(pushSubscription)
                .vapidJWTExpiresAfter(15, TimeUnit.MINUTES)
                .vapidJWTSubject(vapidSubject)
                .pushMessage(messageBytes)
                .ttl(1, TimeUnit.HOURS)
                .build(vapidKeyPair)
                .toRequest();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // 4) 에러 처리 (410/404는 보통 구독 만료/삭제됨)
        if (response.statusCode() == 404 || response.statusCode() == 410) {
            throw new RuntimeException("Subscription is no longer valid (status " + response.statusCode() + ").");
        }
        if (response.statusCode() >= 400) {
            throw new RuntimeException("Push notification failed: " + response.statusCode() + " " + response.body());
        }
    }
}

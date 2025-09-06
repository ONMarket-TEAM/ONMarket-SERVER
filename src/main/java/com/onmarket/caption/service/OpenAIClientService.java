package com.onmarket.caption.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onmarket.common.config.OpenAIProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIClientService {
    private final OpenAIProperties props;
    private final ObjectMapper om = new ObjectMapper();

    private WebClient client() {
        return WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 이미지 URL + 프롬프트를 사용해 JSON 형식 캡션을 생성합니다.
     */
    public Generated callCaptionWithImageUrl(String imageUrl, String userPrompt) {
        if (!props.isEnabled()) {
            return new Generated("(로컬) " + userPrompt + " — 샘플 캡션", null, null, null);
        }
        try {
            // Chat Completions (vision): messages.content 배열에 image_url + text
            var payload = Map.of(
                    "model", props.getChatModel(),               // application.yml의 openai.chatModel 사용 (예: gpt-4.1 / gpt-4o)
                    "temperature", 0.7,
                    "messages", new Object[]{
                            Map.of(
                                    "role", "system",
                                    "content", """
You are a Korean social-media copywriter for small businesses. 
Your task: from an image (food, product, service, interior, etc.) and optional user memo, 
write a concise Instagram caption optimized for *promotion* without sounding spammy.
Must return ONLY valid JSON with keys:
- caption (string, ≤ 800 chars, natural marketing tone in Korean)
- hashtags (array of 6~12 relevant Korean tags; include 2~3 local/업종 태그; no punctuation except '#')
- bestPostTime (string, e.g. "오후 6-8시")
- impactNote (string, 1 short sentence of why this post helps)
- subject (string: what you see; e.g., "마르게리타 피자", "핸드메이드 가죽 카드지갑", "헤어살롱 스타일링")
- confidence (number 0~1: confidence about 'subject')

Rules:
1) Start from what is actually visible in the image (subject). If uncertain, set subject="알 수 없음" and confidence<=0.3.
2) For food/product/service: highlight 1~2 key benefits (e.g., 맛/식감/원산지/핸드메이드/내구성/시그니처/가격대/행사 등).
3) Add a soft CTA at the end (e.g., "방문/문의/예약/DM 주세요", "오늘만 혜택", "근처 오시면 들러보세요").
4) Keep tone warm, trustworthy, and specific. Avoid exaggerated or unverifiable claims.
5) Never include markdown/code fences. Output JSON only.
"""
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", new Object[]{
                                            Map.of("type", "text", "text",
                                                    "이미지를 보고 실제 보이는 대상(subject)을 먼저 파악한 뒤, " +
                                                            "소상공인 홍보용 인스타 캡션을 한국어로 작성하세요. " +
                                                            "사용자 메모(선택): \"" + userPrompt + "\". " +
                                                            "메모에 상호명/지역/이벤트/가격/URL이 있으면 반영하되, " +
                                                            "이미지와 무관한 과장/허위 표현은 금지. " +
                                                            "반드시 지정한 JSON 필드만 포함한 단일 JSON으로 응답하세요."
                                            ),
                                            Map.of("type", "image_url",
                                                    "image_url", Map.of("url", imageUrl))
                                    }
                            )
                    }
            );
            String res = client().post().uri("/chat/completions")
                    .body(BodyInserters.fromValue(payload))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            JsonNode root = om.readTree(res);
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            // 모델이 "```json ...```" 형태로 줄 수도 있어서 정리
            content = content.replaceAll("(?s)```json|```", "").trim();
            JsonNode json = om.readTree(content);
            return new Generated(
                    json.path("caption").asText(""),
                    json.has("hashtags") && json.get("hashtags").isArray() ? json.get("hashtags") : null,
                    json.path("bestPostTime").asText(null),
                    json.path("impactNote").asText(null)
            );
        } catch (Exception e) {
            log.error("OpenAI 호출 실패", e);
            throw new RuntimeException("OpenAI 호출 실패", e);
        }
    }

    public record Generated(String caption, JsonNode hashtags, String bestPostTime, String impactNote) {}
}
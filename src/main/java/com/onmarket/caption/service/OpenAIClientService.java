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
import java.util.ArrayList;
import java.util.List;
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

    /**
     * 다중 이미지 URL들 + 프롬프트를 사용해 JSON 형식 캡션을 생성합니다.
     */
    public Generated callCaptionWithMultipleImageUrls(List<String> imageUrls, String userPrompt) {
        if (!props.isEnabled()) {
            return new Generated("(로컬) " + userPrompt + " — 다중 이미지 샘플 캡션", null, null, null);
        }

        if (imageUrls == null || imageUrls.isEmpty()) {
            throw new IllegalArgumentException("이미지 URL 리스트가 비어있습니다");
        }

        try {
            log.info("다중 이미지 분석 시작: {}개 이미지", imageUrls.size());

            // content 배열 구성: 텍스트 + 여러 이미지들
            List<Object> contentArray = new ArrayList<>();

            // 첫 번째로 텍스트 추가
            contentArray.add(Map.of("type", "text", "text",
                    String.format("다음 %d장의 이미지를 종합적으로 분석하여 소상공인 홍보용 인스타 캡션을 한국어로 작성하세요. " +
                                    "여러 이미지의 내용을 조화롭게 통합하여 하나의 일관된 스토리로 만들어주세요. " +
                                    "사용자 메모(선택): \"%s\". " +
                                    "메모에 상호명/지역/이벤트/가격/URL이 있으면 반영하되, " +
                                    "이미지와 무관한 과장/허위 표현은 금지. " +
                                    "반드시 지정한 JSON 필드만 포함한 단일 JSON으로 응답하세요.",
                            imageUrls.size(), userPrompt)));

            // 각 이미지 URL을 content에 추가
            for (int i = 0; i < imageUrls.size(); i++) {
                contentArray.add(Map.of("type", "image_url",
                        "image_url", Map.of("url", imageUrls.get(i))));
                log.debug("이미지 {}번 추가됨", i + 1);
            }

            var payload = Map.of(
                    "model", props.getChatModel(),
                    "temperature", 0.7,
                    "max_tokens", 1000,  // 다중 이미지 분석이므로 조금 더 많은 토큰 허용
                    "messages", new Object[]{
                            Map.of(
                                    "role", "system",
                                    "content", """
You are a Korean social-media copywriter for small businesses specializing in MULTIPLE IMAGE analysis. 
Your task: analyze multiple images together and create a cohesive Instagram caption that tells a unified story.

When analyzing multiple images:
1) Look for common themes, progressions, or complementary elements
2) Create a narrative that connects all images naturally
3) Highlight the variety/selection if showing different products
4) Use transitional phrases to weave images together (예: "첫 번째부터 마지막까지", "다양한 종류의", "모든 과정이")

Must return ONLY valid JSON with keys:
- caption (string, ≤ 1000 chars, natural marketing tone in Korean that unifies all images)
- hashtags (array of 8~15 relevant Korean tags; include variety/selection tags; no punctuation except '#')
- bestPostTime (string, e.g. "오후 6-8시")
- impactNote (string, 1 short sentence of why this multi-image post helps)
- subject (string: what you see overall; e.g., "다양한 메뉴 구성", "제품 라인업", "서비스 과정")
- confidence (number 0~1: confidence about unified 'subject')

Rules:
1) Analyze ALL images as a cohesive set, not individually
2) Create ONE unified story/message that encompasses all images
3) Emphasize variety, quality, or process shown across images
4) Add a strong CTA that leverages the multiple image advantage
5) Never include markdown/code fences. Output JSON only.
"""
                            ),
                            Map.of("role", "user", "content", contentArray.toArray())
                    }
            );

            String res = client().post().uri("/chat/completions")
                    .body(BodyInserters.fromValue(payload))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(90))  // 다중 이미지 분석이므로 더 긴 타임아웃
                    .block();

            JsonNode root = om.readTree(res);
            String content = root.path("choices").path(0).path("message").path("content").asText("");

            log.debug("OpenAI 응답 원문: {}", content);

            // 모델이 "```json ...```" 형태로 줄 수도 있어서 정리
            content = content.replaceAll("(?s)```json|```", "").trim();
            JsonNode json = om.readTree(content);

            Generated result = new Generated(
                    json.path("caption").asText(""),
                    json.has("hashtags") && json.get("hashtags").isArray() ? json.get("hashtags") : null,
                    json.path("bestPostTime").asText(null),
                    json.path("impactNote").asText(null)
            );

            log.info("다중 이미지 캡션 생성 성공: 캡션 길이 {}", result.caption().length());
            return result;

        } catch (Exception e) {
            log.error("다중 이미지 OpenAI 호출 실패", e);
            throw new RuntimeException("다중 이미지 OpenAI 호출 실패", e);
        }
    }

    public record Generated(String caption, JsonNode hashtags, String bestPostTime, String impactNote) {}
}
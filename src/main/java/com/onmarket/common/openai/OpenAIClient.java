package com.onmarket.common.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onmarket.common.config.OpenAIProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAIClient {
    private final OpenAIProperties props;
    private final ObjectMapper om = new ObjectMapper();

    private WebClient client() {
        var httpClient = reactor.netty.http.client.HttpClient.create()
                .responseTimeout(java.time.Duration.ofSeconds(90))
                .compress(true)
                .doOnConnected(conn -> {
                    conn.addHandlerLast(new io.netty.handler.timeout.ReadTimeoutHandler(90));
                    conn.addHandlerLast(new io.netty.handler.timeout.WriteTimeoutHandler(90));
                });

        var connector = new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient);

        var strategies = org.springframework.web.reactive.function.client.ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(50 * 1024 * 1024)) // 50MB
                .build();

        return WebClient.builder()
                .clientConnector(connector)
                .exchangeStrategies(strategies)
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .build();
    }

    /** 공용 Chat API: JSON 응답 강제 (response_format=json_object) */
    public String chatJson(String system, String user, Double temperature) {
        Map<String, Object> body = Map.of(
                "model", props.getChatModel(),
                "temperature", temperature == null ? 0.3 : temperature,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", user)
                )
        );

        String json = client().post().uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .bodyToMono(String.class)
                .retry(1)
                .block();

        try {
            JsonNode root = om.readTree(json);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("OpenAI chatJson parse error: " + e.getMessage(), e);
        }
    }

    /** 공용 Chat API: 일반 텍스트 응답 */
    public String chatText(String system, String user, Double temperature) {
        Map<String, Object> body = Map.of(
                "model", props.getChatModel(),
                "temperature", temperature == null ? 0.2 : temperature,
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", user)
                )
        );

        String json = client().post().uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .bodyToMono(String.class)
                .retry(1)
                .block();

        try {
            JsonNode root = om.readTree(json);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("OpenAI chatText parse error: " + e.getMessage(), e);
        }
    }

    /** (선택) 이미지 생성 → data URL (gpt-image-1) */
    public String imageDataUrl(String prompt, int w, int h) {
        Map<String, Object> body = Map.of(
                "model", props.getImageModel(),
                "prompt", prompt,
                "size", w + "x" + h,
                "n", 1
        );

        String json = client().post().uri("/images/generations")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .bodyToMono(String.class)
                .retry(1)
                .block();

        try {
            JsonNode root = om.readTree(json);
            JsonNode data0 = root.path("data").get(0);
            String b64 = data0.has("b64_json") ? data0.get("b64_json").asText() : null;
            if (b64 == null && data0.has("url")) {
                byte[] bytes = WebClient.create().get().uri(data0.get("url").asText())
                        .retrieve().bodyToMono(byte[].class).block();
                b64 = Base64.getEncoder().encodeToString(bytes);
            }
            return "data:image/png;base64," + b64;
        } catch (Exception e) {
            throw new RuntimeException("OpenAI imageDataUrl parse error: " + e.getMessage(), e);
        }
    }
}

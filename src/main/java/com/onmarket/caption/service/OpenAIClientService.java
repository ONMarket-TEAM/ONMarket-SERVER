package com.onmarket.caption.service;

import com.onmarket.common.config.OpenAIProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIClientService {

    private static final String DEFAULT_BASE_URL = "https://api.openai.com";
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    private final OpenAIProperties props;

    private WebClient webClient() {
        return WebClient.builder()
                .baseUrl(DEFAULT_BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 여러 장(최대 10) 이미지 URL을 비전 입력으로 보내 캡션 후보 생성
     */
    @SuppressWarnings("unchecked")
    public List<String> generateCaptionsFromImages(
            List<String> imageUrls,
            String systemPrompt,
            String userPrompt,
            int n,
            double temperature
    ) {
        try {
            // user content: 텍스트 + 다중 이미지
            List<Object> userContent = new ArrayList<>();
            userContent.add(Map.of("type", "text", "text", userPrompt));
            for (String url : imageUrls) {
                userContent.add(Map.of(
                        "type", "input_image",
                        "image_url", Map.of("url", url)
                ));
            }

            String model = (props.getChatModel() != null && !props.getChatModel().isBlank())
                    ? props.getChatModel()
                    : "gpt-4o-mini";

            Map<String, Object> body = Map.of(
                    "model", model,
                    "temperature", temperature,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userContent)
                    ),
                    "n", Math.max(1, Math.min(n, 10))
            );

            Map<String, Object> res = webClient().post()
                    .uri("/v1/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                    .block();

            if (res == null) return List.of("⚠️ OpenAI 응답 없음");

            var choices = (List<Map<String, Object>>) res.getOrDefault("choices", List.of());
            List<String> out = new ArrayList<>();
            for (Map<String, Object> c : choices) {
                Map<String, Object> msg = (Map<String, Object>) c.get("message");
                String content = msg != null ? (String) msg.get("content") : null;
                if (content != null && !content.isBlank()) out.add(content.trim());
            }
            return out.isEmpty() ? List.of("⚠️ 생성 결과가 비어 있습니다.") : out;

        } catch (Exception e) {
            log.error("OpenAI Vision 호출 오류", e);
            return List.of("⚠️ 캡션 생성 중 오류가 발생했습니다.");
        }
    }
}
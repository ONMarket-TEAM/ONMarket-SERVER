package com.onmarket.cardnews.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.WaitUntilState;
import com.onmarket.common.config.OpenAIProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import com.microsoft.playwright.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIClientService {

    private final OpenAIProperties props;
    private final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private WebClient webClient() {
        return WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(ExchangeStrategies.builder().codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)).build())
                .build();
    }

    /* -------- 1) 한 행(rawRow)을 요약 -------- */
    public String summarize(String rawRow) {
        // 긴 문자열 자르기(옵션)
        if (rawRow != null && props.getMaxChars() != null && rawRow.length() > props.getMaxChars()) {
            rawRow = rawRow.substring(0, props.getMaxChars());
        }

        var system = """
            너는 금융 상품 설명을 카드뉴스용으로 요약하는 전문가야.
            출력은 꼭 아래 형식 그대로의 마크다운으로:
            # 제목
            - 핵심포인트 3~5개 (간결)
            - 혜택/한도/금리/대상 등 중요한 숫자는 그대로
            - 주의사항 1~2개
            """;

        var user = "다음 한 행의 데이터로 카드뉴스용 요약을 만들어줘:\n\n" + rawRow;

        var req = Map.of(
                "model", props.getChatModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", user)
                ),
                "temperature", 0.4
        );

        var resp = webClient().post()
                .uri("/chat/completions")
                .body(BodyInserters.fromValue(req))
                .retrieve()
                .bodyToMono(Map.class)
                .block(Duration.ofSeconds(60));

        try {
            List<?> choices = (List<?>) resp.get("choices");
            Map<?, ?> first = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) first.get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            log.error("summarize 파싱 실패 resp={}", safeJson(resp), e);
            throw new RuntimeException("OpenAI 요약 응답 파싱 실패", e);
        }
    }

    /* -------- 2) 요약을 기반으로 DALLE 프롬프트 만들기 -------- */
    public String buildBackgroundPrompt(String summaryMarkdown) {
        // 요약의 키워드(상품 성격/대상/분위기)를 배경 컨셉으로 녹여줌
        return """
            Create a clean, modern, non-distracting background for a finance card news.
            Style: flat illustration, soft gradients, high contrast for text overlay.
            Avoid text, logos, charts, and people faces. No watermarks.
            Concept (Korean context): %s
            """.formatted(summaryMarkdown);
    }

    /* -------- 3) DALLE로 배경 PNG(base64) 생성 -------- */
    public byte[] generateImagePng(String prompt) {
        String model = (props.getImageModel() == null || props.getImageModel().isBlank())
                ? "gpt-image-1" : props.getImageModel();

        // Use an allowed size for OpenAI Images
        String size = "1024x1536"; // portrait default (Images API supported)

        var req = Map.of(
                "model", model,
                "prompt", prompt,
                "size", size
        );

        Map<?, ?> resp;
        try {
            resp = webClient().post()
                    .uri("/images/generations")
                    .body(BodyInserters.fromValue(req))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResp -> clientResp.bodyToMono(String.class).map(body ->
                                    new RuntimeException("OpenAI image error " + clientResp.statusCode() + ": " + body)))
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(120));
        } catch (WebClientResponseException e) {
            log.error("OpenAI image HTTP {} body={} ", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("OpenAI image HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        }

        try {
            List<?> data = (List<?>) resp.get("data");
            Map<?, ?> first = (Map<?, ?>) data.get(0);

            // Prefer base64 if present
            Object b64Obj = first.get("b64_json");
            if (b64Obj instanceof String b64 && !b64.isBlank()) {
                return Base64.getDecoder().decode(b64);
            }

            // Fallback to URL if base64 absent
            Object urlObj = first.get("url");
            if (urlObj instanceof String url && !url.isBlank()) {
                byte[] bytes = fetchBytesFromUrl(url);
                if (bytes != null && bytes.length > 0) return bytes;
            }

            throw new IllegalArgumentException("이미지 생성에 실패했습니다(빈 응답).");
        } catch (Exception e) {
            log.error("image 파싱 실패 resp={}", safeJson(resp), e);
            throw new RuntimeException("OpenAI 이미지 응답 파싱 실패", e);
        }
    }

    /**
     * Controller가 리플렉션으로 1순위 탐색하는 시그니처.
     * @param prompt  DALLE 프롬프트
     * @param size    "1024x1536" 같은 문자열
     * @param portrait 세로/가로 힌트(현재는 로직에 사용하지 않지만 시그니처 유지)
     * @return "data:image/png;base64,..." 형식의 data URL
     */
    public String generateBackgroundDataUrl(String prompt, String size, boolean portrait) {
        String model = (props.getImageModel() == null || props.getImageModel().isBlank())
                ? "gpt-image-1" : props.getImageModel();

        // Enforce allowed sizes for gpt-image-1 with graceful fallback
        var allowed = java.util.Set.of("1024x1024", "1024x1536", "1536x1024", "auto");
        if (size == null || !allowed.contains(size)) {
            // If the caller passed a custom WxH (e.g., 1024x1792), map it to the closest supported aspect
            String fallback = portrait ? "1024x1536" : "1536x1024";
            log.warn("Unsupported image size '{}', falling back to {}", size, fallback);
            size = fallback;
        }

        var req = Map.of(
                "model", model,
                "prompt", prompt,
                "size", size
        );

        Map<?, ?> resp;
        try {
            resp = webClient().post()
                    .uri("/images/generations")
                    .body(BodyInserters.fromValue(req))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResp -> clientResp.bodyToMono(String.class).map(body ->
                                    new RuntimeException("OpenAI image error " + clientResp.statusCode() + ": " + body)))
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(120));
        } catch (WebClientResponseException e) {
            log.error("OpenAI image HTTP {} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("OpenAI image HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        }

        try {
            List<?> data = (List<?>) resp.get("data");
            Map<?, ?> first = (Map<?, ?>) data.get(0);

            // Prefer base64 if present
            Object b64Obj = first.get("b64_json");
            if (b64Obj instanceof String b64 && !b64.isBlank()) {
                return "data:image/png;base64," + b64;
            }

            // Fallback to URL if base64 absent
            Object urlObj = first.get("url");
            if (urlObj instanceof String url && !url.isBlank()) {
                byte[] bytes = fetchBytesFromUrl(url);
                if (bytes != null && bytes.length > 0) {
                    String b64 = Base64.getEncoder().encodeToString(bytes);
                    return "data:image/png;base64," + b64;
                }
            }

            throw new IllegalArgumentException("이미지 생성에 실패했습니다(빈 응답).");
        } catch (Exception e) {
            log.error("generateBackgroundDataUrl 파싱 실패 resp={}", safeJson(resp), e);
            throw new RuntimeException("OpenAI 이미지 응답 파싱 실패", e);
        }
    }

    /**
     * 편의 오버로드: width/height를 받아 size 문자열로 위 메서드를 호출
     */
    public String generateBackgroundDataUrl(String prompt, int width, int height) {
        String size = width + "x" + height;
        boolean portrait = height >= width;
        return generateBackgroundDataUrl(prompt, size, portrait);
    }

    /* -------- 4) HTML 합성 → PNG 렌더(Playwright) --------
     * summaryMarkdown과 배경 PNG를 받아 HTML을 합성하고, Playwright로 PNG로 렌더링합니다.
     */
    public byte[] composePoster(String summaryMarkdown, byte[] bgPngBytes) {
        // 아주 단순한 샘플 템플릿(여기서 우리 팀의 실제 HTML 디자인으로 교체 가능)
        String bgDataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(bgPngBytes);

        String html = """
            <!doctype html>
            <html lang="ko">
            <head>
              <meta charset="utf-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1"/>
              <style>
                *{box-sizing:border-box}
                body,html { margin:0; padding:0; width:1024px; height:1536px; }
                .wrap {
                  position:relative; width:1024px; height:1536px; overflow:hidden;
                  font-family: -apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Helvetica,Arial,"Apple SD Gothic Neo","Noto Sans KR",sans-serif;
                }
                .bg { position:absolute; inset:0; background:url('%s') center/cover no-repeat; filter:brightness(0.9); }
                .panel {
                  position:absolute; inset:0; padding:64px 80px; display:flex; flex-direction:column;
                  color:#111; /* 글자가 어두운 배경에 놓일 수도 있으니 필요 시 흰색(#fff)로 바꾸세요 */
                }
                .card {
                  margin-top:auto; margin-bottom:64px; background:rgba(255,255,255,0.9);
                  border-radius:32px; padding:32px 40px; backdrop-filter:saturate(1.2) blur(6px);
                }
                h1 { font-size:56px; line-height:1.15; margin:0 0 24px; font-weight:800; }
                ul { margin:0; padding-left:20px; font-size:34px; line-height:1.5; }
                .footer { position:absolute; bottom:24px; right:32px; font-size:22px; opacity:.7 }
              </style>
            </head>
            <body>
              <div class="wrap">
                <div class="bg"></div>
                <div class="panel">
                  <div class="card" id="content"></div>
                  <div class="footer">ONMarket</div>
                </div>
              </div>
              <script>
                // 서버에서 받은 마크다운을 간단하게 변환(가벼운 변환; 팀에서 정식 렌더러 쓰면 교체하세요)
                const md = `%s`;
                function mdToHtml(m){
                  // 아주 얕은 마크다운 파서 (H1 + bullet 정도)
                  const lines = m.split(/\\r?\\n/);
                  let html = "";
                  for (const line of lines) {
                    if (line.startsWith("# ")) {
                      html += "<h1>"+ line.substring(2).replace(/</g,"&lt;") +"</h1>";
                    } else if (line.startsWith("- ")) {
                      if (!html.endsWith("</ul>")) html += "<ul>";
                      html += "<li>"+ line.substring(2).replace(/</g,"&lt;") +"</li>";
                    } else if (line.trim()==="") {
                      if (html.endsWith("</li>")) html += "</ul>";
                    }
                  }
                  if (html.endsWith("</li>")) html += "</ul>";
                  return html;
                }
                document.querySelector(".bg").style.backgroundImage = "url('%s')";
                document.getElementById("content").innerHTML = mdToHtml(md);
              </script>
            </body>
            </html>
            """.formatted(bgDataUrl, escapeJs(summaryMarkdown), bgDataUrl);

        // Playwright로 렌더링해서 PNG 추출
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext ctx = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(1024, 1536)
                    .setDeviceScaleFactor(2)); // DALLE 기본 비율과 맞춤
            Page page = ctx.newPage();
            page.setContent(html, new Page.SetContentOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
            byte[] png = page.screenshot(new Page.ScreenshotOptions().setFullPage(false).setType(ScreenshotType.PNG));
            ctx.close();
            browser.close();
            return png;
        }
    }

    /* -------------------- 유틸 -------------------- */
    private byte[] fetchBytesFromUrl(String url) {
        try {
            return webClient()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block(Duration.ofSeconds(120));
        } catch (Exception e) {
            log.error("이미지 URL 다운로드 실패 url={}", url, e);
            return null;
        }
    }

    private String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("$", "\\$")
                .replace("\n", "\\n");
    }

    private String safeJson(Object o) {
        try { return mapper.writeValueAsString(o); }
        catch (Exception e) { return String.valueOf(o); }
    }
}
// src/main/java/com/onmarket/cardnews/service/HtmlTemplateService.java
package com.onmarket.cardnews.service;

import com.onmarket.cardnews.dto.PosterConfig;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class HtmlTemplateService {

    // ${...} 플레이스홀더를 replace로 치환합니다. (String.format/ formatted 사용 안 함)
    private static final String TEMPLATE = """
<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <title>${TITLE}</title>
  <style>
    :root{
      --accent: ${ACCENT};
      --brand:  ${BRAND};
    }
    *{ box-sizing: border-box; }
    html,body{ margin:0; padding:0; }
    body{
      width:100%;
      height:100%;
      font-family: Pretendard, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Noto Sans KR", "Apple SD Gothic Neo", "Helvetica Neue", Arial, "Malgun Gothic", sans-serif;
      color:#111;
      background: #fff;
      background-image: url('${BG_URL}');
      background-repeat:no-repeat;
      background-size: cover;
      background-position: center;
    }
    .wrap{
      display:grid;
      grid-template-columns: 1fr;
      gap: 14px;
      padding: 22px 18px 28px;
      background: linear-gradient(180deg, rgba(255,255,255,.88) 0%, rgba(255,255,255,.96) 35%, #fff 70%);
      min-height: 100%;
    }
    .badge{
      display:inline-block;
      font-size: 12px;
      font-weight: 700;
      color:#fff;
      background: var(--brand);
      padding: 6px 10px;
      border-radius: 999px;
      letter-spacing: .3px;
    }
    h1{
      margin: 2px 0 4px;
      font-size: 28px;
      line-height: 1.25;
      letter-spacing: -0.3px;
    }
    h2{
      margin: 0 0 6px;
      color:#444;
      font-size: 16px;
      font-weight: 500;
    }
    .card{
      background:#fff;
      border: 2px solid rgba(0,0,0,.05);
      border-radius: 18px;
      padding: 14px 16px;
      box-shadow: 0 10px 28px rgba(17,37,94,.06);
    }
    .section-title{
      margin: 0 0 8px;
      color: var(--accent);
      font-weight: 800;
      font-size: 18px;
    }
    ul{
      margin: 8px 0 0 18px;
      padding: 0;
      list-style: disc;
    }
    li{ margin: 4px 0; }
    .note{
      margin-top: 8px;
      color:#6b7280;
      font-size: 13px;
      line-height: 1.5;
    }
    .footer{
      margin-top: 8px;
      display:flex; justify-content: space-between; align-items: center;
      font-size: 12px; color:#9ca3af;
    }
    .brand-dot{ width:10px; height:10px; border-radius:50%; background:var(--brand); display:inline-block; vertical-align:middle; margin-right:6px; }
    .grid{
      display:grid;
      grid-template-columns: 1fr;
      gap: 10px;
    }
    /* 간단 반응형 */
    @media (min-width:480px){
      .grid{ grid-template-columns: 1fr 1fr; }
    }
  </style>
</head>
<body>
  <div class="wrap">
    <div>
      ${BADGE_BLOCK}
      <h1>${TITLE}</h1>
      <h2>${SUBTITLE}</h2>
    </div>

    <div class="grid">
      ${SECTIONS}
    </div>

    <div class="footer">
      <div><span class="brand-dot"></span>ONMARKET · 자동 생성 카드뉴스</div>
      <div>배경: DALLE | 폰트: Pretendard</div>
    </div>
  </div>
</body>
</html>
""";

    public String renderHtml(PosterConfig cfg, String bgDataUrl) {
        Objects.requireNonNull(cfg, "PosterConfig must not be null");

        String accent = safe(cfg.getTheme() != null ? cfg.getTheme().getAccent() : null, "#0ea5b7");
        String brand  = safe(cfg.getTheme() != null ? cfg.getTheme().getBrand()  : null, "#2563eb");

        String title    = esc(safe(cfg.getTitle(), ""));
        String subtitle = esc(safe(cfg.getSubtitle(), ""));
        String badge    = esc(safe(cfg.getBadge(), ""));

        String badgeBlock = badge.isEmpty() ? "" : "<span class=\"badge\">" + badge + "</span>";

        String sectionsHtml = buildSections(cfg.getSections());

        return TEMPLATE
                .replace("${ACCENT}", accent)
                .replace("${BRAND}", brand)
                .replace("${BG_URL}", bgDataUrl == null ? "" : bgDataUrl)
                .replace("${TITLE}", title)
                .replace("${SUBTITLE}", subtitle)
                .replace("${BADGE_BLOCK}", badgeBlock)
                .replace("${SECTIONS}", sectionsHtml);
    }

    private String buildSections(List<PosterConfig.Section> sections) {
        if (sections == null || sections.isEmpty()) return "";

        return sections.stream()
                .map(s -> {
                    String heading = esc(safe(s.getHeading(), ""));
                    String text    = esc(safe(s.getText(), ""));
                    String bullets = (s.getBullets() == null || s.getBullets().isEmpty())
                            ? ""
                            : s.getBullets().stream()
                            .map(b -> "<li>" + esc(safe(b, "")) + "</li>")
                            .collect(Collectors.joining());
                    String ul = bullets.isEmpty() ? "" : "<ul>" + bullets + "</ul>";
                    String note = text.isEmpty() ? "" : "<div class=\"note\">" + text + "</div>";

                    return """
                           <div class="card">
                             %s
                             %s
                           </div>
                           """.formatted(
                            heading.isEmpty() ? "" : "<h3 class=\"section-title\">" + heading + "</h3>",
                            ul.isEmpty() ? note : (ul + note)
                    );
                })
                .collect(Collectors.joining("\n"));
    }

    private static String safe(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }

    private static String esc(String s) {
        if (s == null) return "";
        // 아주 간단한 HTML escape (필요시 Apache Commons Text로 교체 가능)
        return s.replace("&", "&amp;")
                .replace("<","&lt;")
                .replace(">","&gt;")
                .replace("\"","&quot;")
                .replace("'","&#39;");
    }
}
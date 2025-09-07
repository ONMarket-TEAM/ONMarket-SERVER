package com.onmarket.cardnews.service;

import com.onmarket.cardnews.dto.PosterConfig;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class HtmlTemplateService {

    private static final String TEMPLATE = """
<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <title>${TITLE}</title>
  <style>
    *{box-sizing:border-box}
    html,body{margin:0;padding:0}
    :root{
      --accent:${ACCENT};
      --brand:${BRAND};
      --title-offset: 44px;     /* 제목 오프셋 */
      --bg-y: ${BG_Y};          /* 배경 세로 위치(운영사/배지별 미세 튜닝) */
    }
    body{
      width:1024px;height:1536px;
      font-family:Pretendard,-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,"Noto Sans KR","Apple SD Gothic Neo","Helvetica Neue",Arial,"Malgun Gothic",sans-serif;
      background: url('${BG_URL}') center/cover no-repeat; /* 배경은 body에 고정 */
      background-position: center var(--bg-y);
      color:#0b1324; display:flex; flex-direction:column; position:relative;
      padding-bottom: 220px; /* 하단 고정 요소들과 겹침 방지 */
    }

    /* 상단 바 */
    .topbar{
      width:100%; max-width:980px; margin:28px auto 0; padding:0 24px;
      display:flex; justify-content:space-between; align-items:center; gap:16px;
    }

    /* 배지 */
    .badge{
      display:inline-flex; align-items:center; gap:10px;
      font-weight:900; font-size:22px; color:#fff;
      padding:10px 18px; border-radius:16px; letter-spacing:.2px;
      max-width:55%; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;
      border:2px solid rgba(255,255,255,.95);
    }
    .badge.loan{ background: var(--brand); }
    .badge.support{ background: #10b981; }

    /* 워터마크 */
    .maker{
      display:inline-flex; align-items:center; gap:8px;
      padding:8px 12px; border-radius:12px;
      color:rgba(255,255,255,.95);
      background:rgba(0,0,0,.28);
      border:1px solid rgba(255,255,255,.55);
      font-weight:800; font-size:14px; letter-spacing:.4px;
      backdrop-filter: blur(4px);
    }
    .maker-dot{width:8px;height:8px;border-radius:50%;background:var(--accent);display:inline-block}

    /* ====== 상단 타이틀 & 설명 ====== */
    .hero-plain{
      width:100%; max-width:980px; margin: var(--title-offset) auto 0; padding:0 24px;
      text-align:center; pointer-events:none;
    }
    .title-plain{
      margin:0; font-weight:900; font-size:96px; line-height:1.02; letter-spacing:-1px; color:#fff;
      -webkit-text-stroke: 1px rgba(0,0,0,.15);
      word-break:keep-all;
      display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden;
    }
    .title-desc{
      margin:12px auto 0; max-width:900px; padding:0 8px;
      font-size:24px; line-height:1.45; font-weight:700;
      color:#f8fafc; text-shadow: 0 2px 8px rgba(0,0,0,.35);
      word-break:keep-all;
    }

    /* ====== 하단 3 알약형 카드 (타이틀 바로 아래) ====== */
    .pills{
      width:100%; max-width:980px;
      margin: 36px auto 0;          /* 제목/설명 바로 아래에서 '약간 아래'로 */
      padding:0 24px;
      display:grid; grid-template-columns: repeat(3, 1fr); gap:18px;
    }
    .pill{
      border-radius:28px; overflow:hidden;
      box-shadow:0 14px 28px rgba(17,37,94,.10), 0 2px 6px rgba(17,37,94,.05);
      background:#fff; border:1px solid rgba(17,37,94,.06);
      display:flex; flex-direction:column; min-height:260px;
    }
    .pill-head{
      padding:18px 16px 14px; text-align:center; color:#fff; font-weight:900;
      background: var(--brand);
      font-size:30px; letter-spacing:-0.3px;
    }
    /* === 알약 본문 가독성 개선 + 점 불릿 추가 === */
    .pill-body{
      padding:20px 20px 24px;
      display:flex;
      flex-direction:column;
      gap:12px;                 /* 줄 간 간격 ↑ */
    }
    
    .pill-line{
      position:relative;
      padding-left:18px;        /* 불릿 여백 */
      font-size:20px;           /* 글자 크기 ↑ */
      line-height:1.55;         /* 가독성 ↑ */
      color:#0b1324;
      letter-spacing:-.2px;
    }
    
         /* 점(도트) 불릿 */
    .pill-line::before{
      content:"";
      position:absolute;
      left:0;                   /* 텍스트 앞 */
      top:0.9em;
      width:7px; height:7px;
      border-radius:50%;
      background:var(--brand);  /* 브랜드 컬러 도트 */
      box-shadow:0 0 0 3px rgba(17,37,94,.06); /* 얇은 링으로 더 눈에 띄게 */
    }

    /* 기존 강조 스타일 유지 */
    .pill-line .em{  font-weight:900; color:var(--brand);  }
    .pill-line .em2{ font-weight:900; color:var(--accent); }
    /* ====== 약관/유의(하단 고정) ====== */
    .fineprint{
      position:absolute; left:0; right:0; bottom:110px; /* footer 위에 고정 */
      width:100%; max-width:980px; margin:0 auto; padding:0 24px;
    }
    .fineprint-inner{ background:rgba(255,255,255,.96); border:1px solid rgba(17,37,94,.08); border-radius:16px; box-shadow:0 8px 18px rgba(17,37,94,.06); }
    .fineprint-text{ padding:16px 18px; font-size:18px; line-height:1.55; color:#4b5563; word-break:keep-all; }

    /* ====== 바닥 중앙 운영기관 ====== */
    .footerbar{
      position:absolute; left:0; right:0; bottom:14px;
      width:100%; text-align:center;
      color:#fff; font-size:30px; 
    }
    .footerbar .dot{ display:none; }
  </style>
</head>
<body>

  <div class="topbar">
    ${BADGE_BLOCK}
    ${MAKER_BLOCK}
  </div>

  <!-- 상단 제목 + 설명 -->
  ${HERO_BLOCK}

  <!-- 알약 3개: 이제 제목 바로 아래 -->
  <div class="pills">
    ${PILLS}
  </div>

  <!-- 약관/유의 -->
  ${FINEPRINT_BLOCK}

  <!-- 바닥 운영기관 1회 -->
  <div class="footerbar">${FOOTER_BLOCK}</div>
</body>
</html>
""";

    public String renderHtml(PosterConfig cfg, String bgUrl) {
        Objects.requireNonNull(cfg, "PosterConfig must not be null");

        String[] p = fallbackPalette(cfg);
        String accent = pick(cfg.getTheme()!=null ? cfg.getTheme().getAccent() : null, p[1]);
        String brand  = pick(cfg.getTheme()!=null ? cfg.getTheme().getBrand()  : null, p[0]);

        // 제목: 꼬리표 제거
        String titleRaw = pick(cfg.getTitle(), "");
        String titleOnly = esc(stripGuideWord(titleRaw));

        String badge    = esc(pick(cfg.getBadge(), ""));
        String operator = esc(cfg.getOperator()!=null ? pick(cfg.getOperator().getName(), "") : "");

        String badgeClass = "loan";
        String bLower = badge.replace(" ", "");
        if (bLower.contains("정부지원금") || bLower.contains("정책지원")) badgeClass = "support";

        String badgeBlock = badge.isBlank() ? "" : "<div class='badge " + badgeClass + "'>" + badge + "</div>";
        String makerBlock = "<div class='maker' aria-label='Made by ONMARKET'><span class='maker-dot'></span><span>ONMARKET</span></div>";

        // 설명 문구: {주관사}에서 운영하는 {상대}를 위한 {대출} 안내
        String rawOperator = cfg.getOperator()!=null ? pick(cfg.getOperator().getName(), "") : "";
        String rawTitle    = stripGuideWord(pick(cfg.getTitle(), ""));
        String tagline = esc(buildTaglineSmart(cfg, rawOperator, rawTitle));
        String heroBlock = titleOnly.isBlank() ? "" :
                """
                <div class="hero-plain">
                  <h1 class="title-plain">""" + titleOnly + "</h1>\n" +
                        (tagline.isBlank()? "" : "<p class=\"title-desc\">" + tagline + "</p>") +
                        "</div>\n";

        String pills = buildPills(cfg.getSections());
        String fineprintBlock = buildFineprintBlock(cfg);
        String footerBlock = operator.isBlank() ? "" :
                "<div class='operator'><span>" + operator + "</span></div>";

        String bgY = computeBgY(cfg);

        return TEMPLATE
                .replace("${ACCENT}", accent)
                .replace("${BRAND}", brand)
                .replace("${BG_URL}", bgUrl == null ? "" : bgUrl)
                .replace("${BG_Y}", bgY)
                .replace("${TITLE}", titleOnly)
                .replace("${BADGE_BLOCK}", badgeBlock)
                .replace("${MAKER_BLOCK}", makerBlock)
                .replace("${HERO_BLOCK}", heroBlock)
                .replace("${PILLS}", pills)
                .replace("${FINEPRINT_BLOCK}", fineprintBlock)
                .replace("${FOOTER_BLOCK}", footerBlock);
    }

    /** 알약 3개 */
    private String buildPills(List<PosterConfig.Section> sections) {
        if (sections == null || sections.isEmpty()) return "";
        return sections.stream().limit(3).map(s -> {
            String head = esc(pick(s.getHeading(), ""));
            String body;
            if (s.getBullets()!=null && !s.getBullets().isEmpty()) {
                body = s.getBullets().stream()
                        .map(b -> "<div class='pill-line'>"+ emphasize(esc(pick(b,""))) +"</div>")
                        .collect(Collectors.joining());
            } else {
                body = "<div class='pill-line'>" + emphasize(esc(pick(s.getText(),""))) + "</div>";
            }
            return """
            <div class="pill">
              <div class="pill-head">%s</div>
              <div class="pill-body">%s</div>
            </div>
            """.formatted(head, body);
        }).collect(Collectors.joining("\n"));
    }

    /** 하단 주의/문의 안내 */
    private String buildFineprintBlock(PosterConfig cfg){
        String badge = cfg.getBadge()==null? "" : cfg.getBadge();
        String b = badge.replace(" ", "");
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (b.contains("정부지원금") || b.contains("정책지원")) {
            lines.add("상세 조건과 일정은 공고문을 참조하세요");
            lines.add("기관 심사 및 예산 소진 시 조기 종료될 수 있습니다");
            lines.add("문의: 주관기관 콜센터 또는 누리집");
        } else {
            lines.add("실제 한도·금리는 심사에 따라 달라질 수 있습니다");
            lines.add("수수료·부대비용(인지세 등)은 약정서로 확인하세요");
            lines.add("문의: 부산은행 영업점·모바일 앱 고객센터");
        }
        String paragraph = esc(String.join(" · ", lines));
        return """
        <div class="fineprint">
          <div class="fineprint-inner">
            <div class="fineprint-text">%s</div>
          </div>
        </div>
        """.formatted(paragraph);
    }

    private String[] fallbackPalette(PosterConfig cfg){
        String badge = cfg.getBadge()==null? "" : cfg.getBadge();
        String b = badge.replace(" ", "");
        if (b.contains("정부지원금") || b.contains("정책지원")) {
            return new String[]{"#10b981", "#3b82f6"}; // green / blue
        }
        return new String[]{"#2563eb", "#06b6d4"};     // blue / cyan
    }

    private static String pick(String v, String def){ return (v==null||v.isBlank())?def:v; }

    private static String esc(String s){
        if (s==null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#39;");
    }

    /* 강조 로직 */
    private static String emphasize(String escaped){
        if (escaped==null || escaped.isBlank()) return escaped;
        String out = escaped.replaceAll("\\*\\*(.+?)\\*\\*", "<span class='em'>$1</span>");
        out = replaceFirstN(PCT, out, "<span class='em2'>$0</span>", 2);
        out = replaceFirstN(AMOUNT, out, "<span class='em2'>$0</span>", 2);
        out = replaceFirstN(AGE, out, "<span class='em2'>$0</span>", 1);
        out = replaceFirstN(KEYWORDS, out, "<span class='em'>$0</span>", 2);
        return out;
    }
    private static final Pattern PCT = Pattern.compile("\\d+(?:\\.\\d+)?\\s*%");
    private static final Pattern AMOUNT = Pattern.compile("(?:\\d{1,3}(?:,\\d{3})+|\\d+)(?:\\s*)(?:원|만원|억원|천원|만원대|억원대)");
    private static final Pattern AGE = Pattern.compile("만\\s*\\d+\\s*세");
    private static final Pattern KEYWORDS = Pattern.compile("금리|한도|KCB|신용평가|영업점|스마트폰|모바일|마이너스한도대출|부산은행");
    private static String replaceFirstN(Pattern p, String src, String replacement, int n){
        Matcher m = p.matcher(src);
        StringBuffer sb = new StringBuffer(src.length() + 32);
        int cnt = 0;
        while (m.find()) {
            if (cnt < n) {
                m.appendReplacement(sb, Matcher.quoteReplacement(replacement.replace("$0", m.group())));
                cnt++;
            } else {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /* 제목 꼬리표 제거(‘안내’ 등) */
    private static String stripGuideWord(String title){
        if (title == null) return "";
        String t = title.trim();
        t = t.replaceAll("\\s*안내(?:문)?(?:입니다)?\\s*$", "");
        return t;
    }

    /** {주관사}에서 운영하는 {상대}를 위한 {대출} 안내 */
    private String buildTaglineSmart(PosterConfig cfg, String operator, String title) {
        String sub = pick(cfg.getSubtitle(), "").trim();
        if (!sub.isBlank()) return sub;

        // '누가 대상인가요?'의 첫 줄을 대상 문구로 사용
        String audience = "";
        if (cfg.getSections() != null) {
            for (PosterConfig.Section s : cfg.getSections()) {
                String heading = s.getHeading()==null? "" : s.getHeading();
                if (heading.contains("누가 대상")) {
                    if (s.getBullets()!=null && !s.getBullets().isEmpty()) {
                        audience = sanitizeAudience(s.getBullets().get(0));
                    } else if (s.getText()!=null) {
                        audience = sanitizeAudience(s.getText());
                    }
                    break;
                }
            }
        }
        if (audience.isBlank()) audience = "고객";

        String loanWord = "대출";
        if (title != null && !title.isBlank()) {
            Matcher m = Pattern.compile("([가-힣A-Za-z0-9]+\\s*대출)").matcher(title);
            if (m.find()) loanWord = m.group(1).trim();
        }

        String opPart = (operator==null || operator.isBlank()) ? "" : operator + "에서 운영하는 ";
        return opPart + audience + "을 위한 " + loanWord + " 안내";
    }

    private String sanitizeAudience(String raw){
        if (raw == null) return "";
        String a = raw.trim();
        a = a.replaceAll("신청\\s*가능.*$", "");
        a = a.replaceAll("대상.*$", "");
        a = a.replaceAll("\\s*등$", "");
        a = a.replaceAll("\\s*및\\s*.*$", "");
        if (a.length() > 22) a = a.substring(0, 22);
        return a;
    }

    /** 배경 Y 위치 결정(작을수록 위로) */
    private String computeBgY(PosterConfig cfg){
        String bg = "26%";
        String badge = cfg.getBadge()==null? "" : cfg.getBadge();
        if (badge.contains("정부지원금") || badge.contains("정책지원")) bg = "24%";
        String op = cfg.getOperator()!=null ? cfg.getOperator().getName() : "";
        if (op==null) op="";
        if (op.contains("서민금융진흥원")) return "22%";
        if (op.contains("소상공인시장진흥공단")) return "24%";
        if (op.contains("신용보증재단")) return "24%";
        String[] banks = {"IBK기업은행","국민은행","신한은행","우리은행","하나은행","농협은행주식회사","토스뱅크","카카오뱅크","케이뱅크","한국산업은행","한국스탠다드차타드은행"};
        for (String b : banks) if (op.contains(b)) return "24%";
        return bg;
    }
}
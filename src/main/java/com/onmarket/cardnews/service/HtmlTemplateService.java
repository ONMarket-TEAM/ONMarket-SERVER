package com.onmarket.cardnews.service;

import com.onmarket.cardnews.dto.PosterConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class HtmlTemplateService {

    /** 디자인 아이콘/푸터 이미지 베이스 경로 */
    private static final String DESIGN_BASE = "https://onmarket-userfiles.s3.ap-northeast-2.amazonaws.com/design";
    private static final Pattern KEYWORDS2 = Pattern.compile("지원|제외|기간|금리|한도");
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
      --title-offset: 44px;
      --bg-y: ${BG_Y};
      --band: color-mix(in srgb, var(--brand) 14%, white);
    }
    body{
      width:1024px;height:1536px;
      font-family:Pretendard,-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,"Noto Sans KR","Apple SD Gothic Neo","Helvetica Neue",Arial,"Malgun Gothic",sans-serif;
      background: url('${BG_URL}') center/cover no-repeat;
      background-position: center var(--bg-y);
      color:#0b1324; display:flex; flex-direction:column; position:relative;
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

    /* 상단 ONMARKET 로고(배경·테두리 제거) */
    .maker{
      display:inline-flex; align-items:center; gap:8px;
      padding:0; background:none; border:none;
    }
    .maker img{ height:64px; object-fit:contain; }  /* 기존 42px → 크게 */

    /* ====== 상단 타이틀 & 설명 ====== */
    .hero-plain{
      width:100%; max-width:980px; margin: var(--title-offset) auto 0; padding:0 24px;
      text-align:center; pointer-events:none;
    }
    .title-plain{
      margin:0; font-weight:900; font-size:96px; line-height:1.02; letter-spacing:-1px; color:#0b1324;
      text-shadow:0 2px 0 rgba(255,255,255,.6);
      word-break:keep-all;
      display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden;
    }
    .title-desc{
      margin:12px auto 0; max-width:900px; padding:0 8px;
      font-size:26px; line-height:1.45; font-weight:700;
      color:#1f2937;
      word-break:keep-all;
    }

    /* ====== 콘텐츠 밴드 ====== */
    .content-band{
      margin-top:12px;
      width:100%; max-width:980px;
      margin: 35px auto 0; padding:16px 24px 22px;
      border-radius:28px;
      background: var(--band);
      box-shadow: inset 0 1px 0 rgba(255,255,255,.6), 0 14px 32px rgba(17,37,94,.10);
    }

    /* ====== 3줄 가로바 ====== */
    .rows{ width:100%; display:flex; flex-direction:column; gap:16px; }
    .row{
      display:grid; grid-template-columns: 220px 1fr; gap:18px;
      background:#fff; border:1px solid rgba(17,37,94,.06);
      border-radius:24px; overflow:hidden;
      box-shadow:0 14px 28px rgba(17,37,94,.08), 0 2px 6px rgba(17,37,94,.05);
    }
    .row-thumb{
      height:148px; margin:12px; border-radius:18px;
      background-size:cover; background-repeat:no-repeat;
      background-position: center calc(var(--bg-y) + 6%);
      box-shadow: inset 0 0 0 1px rgba(255,255,255,.6);
    }
    .pos-left{  background-position: 28% calc(var(--bg-y) + 6%); }
    .pos-center{ background-position: 50% calc(var(--bg-y) + 6%); }
    .pos-right{ background-position: 72% calc(var(--bg-y) + 6%); }

    /* 아이콘 전용(흰 배경, 꽉차게) */
    .row-thumb.icon{
      background-color:#fff;
      background-size: 86% auto;
      background-position: center center;
      border:1px solid rgba(17,37,94,.06);
      box-shadow: inset 0 0 0 1px rgba(255,255,255,.9);
    }

    .row-content{ padding:18px 18px 16px 0; }
    .row-title{ font-weight:900; font-size:26px; letter-spacing:-.2px; color:#0b1324; margin:2px 0 16px; }
    .row-body{ display:flex; flex-direction:column; gap:12px; }
    .row-body .line{ position:relative; padding-left:16px; font-size:20px; line-height:1.52; color:#0b1324; }
    .row-body .line::before{ content:""; position:absolute; left:0; top:0.85em; width:7px; height:7px; border-radius:50%; background:var(--brand); box-shadow:0 0 0 3px rgba(17,37,94,.06); }
    .row-body .line .kw { font-weight:700; color:var(--brand); }
    .row-body .line .kw2{ font-weight:700; color:var(--accent); }
    .rows .row:first-child .row-title{
      margin-bottom: 24px;
    }
    /* ====== 약관/유의(하단) - 안쪽 좌/우 로고 */
    .fineprint{
      width:100%; max-width:980px; margin:40px auto 0; padding:0 24px;
    }
    .fineprint-inner{
      position:relative;
      background:rgba(255,255,255,.96);
      border:1px solid rgba(17,37,94,.08);
      border-radius:16px;
      box-shadow:0 8px 18px rgba(17,37,94,.06);
      padding:16px 220px; /* 좌우 로고 공간 확보 */
    }
    .fineprint-inner::before,
    .fineprint-inner::after{
      content:"";
      position:absolute;
      top:10px;                          /* ← 위아래를 고정해서 세로로 꽉 차게 */
      bottom:10px;
      width:200px;                        /* ← 가로 폭 크게 (원하면 220~240까지 가능) */
      background-repeat:no-repeat;
      background-size:contain;            /* 비율 유지하면서 요소 박스 안에 맞춤 */
      background-position:center;
      opacity:.9;
    }
    .fineprint-inner::before{ left:20px;  background-image:url('${DESIGN_BASE}/footer1.png'); }
    .fineprint-inner::after{  right:20px; background-image:url('${DESIGN_BASE}/footer2.png'); }
    .fineprint-text{ font-size:18px; line-height:1.55; color:#4b5563; word-break:keep-all; }

    /* ====== 바닥 운영기관 (이미지+텍스트) ====== */
    .footerbar{
       position:absolute; left:0; right:0; bottom:14px;
       width:100%; text-align:center;
     }
     .footerbar .operator{
       display:inline-flex; align-items:center; gap:12px;
       padding:0; background:none; border:none; /* 투명 */
     }
     .footerbar .operator img{ height:40px; object-fit:contain; } /* 로고 크게 */
     .footerbar .operator span{ font-size:28px; font-weight:500; color:#0b1324; }
    
    .hero-image{
      position:relative;
      width:100%;
      max-width:980px;
      height:520px;                 /* ✅ 훨씬 크게 */
      margin:-40px auto 0;          /* ✅ 위로 당겨서 제목 영역과 겹치게 */
      border-radius:24px;
      background: rgba(255,255,255,0.72);   /* 반투명 흰 프레임 유지 */
      box-shadow: 0 10px 24px rgba(17,37,94,.08);
      overflow:hidden;
      z-index:1;                    /* 제목 아래/위 배치 조절용 */
    }

    /* 실제 together.png 넣기: 전부 보이게(contain) + 반복 금지 + 오른쪽 살짝 치우침 */
    .hero-image::after{
      content:"";
      position:absolute;
      inset:8px;                    /* 프레임(여백). 더 크게 보이려면 6px/4px로 줄여도 됨 */
      border-radius:16px;
      background-image:url('${DESIGN_BASE}/together.png');
      background-repeat:no-repeat;  /* ✅ 반복 금지 */
      background-size:contain;      /* ✅ 전부 보이게 (크롭 없음) */
      background-position: 62% 50%; /* ✅ 오른쪽으로 약간 이동 (필요시 60~68%로 조절) */
    }
  </style>
</head>
<body>

  <div class="topbar">
    ${BADGE_BLOCK}
    ${MAKER_BLOCK}
  </div>

  <!-- 상단 제목 + 설명 -->
  ${HERO_BLOCK}
  <div class="hero-image"></div>

  <!-- 타이틀 밑 얇은 네모칸(삭제됨) -->
  ${HERO_STRIP}

  <!-- 콘텐츠 밴드 -->
  <div class="content-band">
    <div class="rows">
      ${ROWS}
    </div>
  </div>

  <!-- 약관/유의 -->
  ${FINEPRINT_BLOCK}

  <!-- 바닥 운영기관 -->
  <div class="footerbar">${FOOTER_BLOCK}</div>
</body>
</html>
""";


    public String renderHtml(PosterConfig cfg, String bgUrl) {
        Objects.requireNonNull(cfg, "PosterConfig must not be null");

        String[] p = fallbackPalette(cfg);
        String accent = pick(cfg.getTheme()!=null ? cfg.getTheme().getAccent() : null, p[1]);
        String brand  = pick(cfg.getTheme()!=null ? cfg.getTheme().getBrand()  : null, p[0]);

        String titleRaw = pick(cfg.getTitle(), "");
        String titleOnly = esc(stripGuideWord(titleRaw));

        String badge    = esc(pick(cfg.getBadge(), ""));
        String operator = esc(cfg.getOperator()!=null ? pick(cfg.getOperator().getName(), "") : "");

        String badgeClass = "loan";
        String bLower = badge.replace(" ", "");
        if (bLower.contains("정부지원금") || bLower.contains("정책지원")) badgeClass = "support";

        String badgeBlock = badge.isBlank() ? "" : "<div class='badge " + badgeClass + "'>" + badge + "</div>";

        // 상단 onmarket 로고(배경 없이)
        String makerBlock = "<div class='maker' aria-label='Made by ONMARKET'>" +
                "<img src='" + DESIGN_BASE + "/onmarket_logo.png' alt='ONMARKET'/>" +
                "</div>";

        // 설명
        String rawOperator = cfg.getOperator()!=null ? pick(cfg.getOperator().getName(), "") : "";
        String rawTitle    = stripGuideWord(pick(cfg.getTitle(), ""));
        String tagline = esc(buildTaglineSmart(cfg, rawOperator, rawTitle));
        String heroBlock = titleOnly.isBlank() ? "" :
                """
                <div class="hero-plain">
                  <h1 class="title-plain">""" + titleOnly + "</h1>\n" +
                        (tagline.isBlank()? "" : "<p class=\"title-desc\">" + tagline + "</p>") +
                        "</div>\n";

        String rows = buildRows(cfg.getSections(), bgUrl);

        String fineprintBlock = buildFineprintBlockByOperator(cfg);

        // 운영사 로고 + 텍스트
        String operatorLogo = operatorLogoForName(rawOperator);
        String footerBlock = operator.isBlank() ? "" :
                "<div class='operator'>" +
                        "<img src='" + DESIGN_BASE + "/" + operatorLogo + "' alt=''/>" +
                        "<span>" + operator + "</span></div>";

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
                .replace("${HERO_STRIP}", "")          // 타이틀 밑 네모칸 제거
                .replace("${ROWS}", rows)
                .replace("${FINEPRINT_BLOCK}", fineprintBlock)
                .replace("${FOOTER_BLOCK}", footerBlock)
                .replace("${DESIGN_BASE}", DESIGN_BASE);
    }


    /** 가로바 3줄: 왼쪽 썸네일을 항목명에 따라 아이콘 대체 */
    private String buildRows(List<PosterConfig.Section> sections, String bgUrl) {
        if (sections == null || sections.isEmpty()) return "";
        String[] pos = {"pos-left","pos-center","pos-right"};
        return sections.stream().limit(3).map((s) -> {
            int idx = sections.indexOf(s);
            String head = esc(pick(s.getHeading(), ""));
            String body;
            if (s.getBullets()!=null && !s.getBullets().isEmpty()) {
                body = s.getBullets().stream()
                        .map(b -> "<div class='line'>"+ emphasize(esc(pick(b,""))) +"</div>")
                        .collect(Collectors.joining());
            } else {
                body = "<div class='line'>" + emphasize(esc(pick(s.getText(),""))) + "</div>";
            }

            // 헤딩 키워드로 아이콘 결정
            String iconUrl = iconForHeading(head);
            boolean useIcon = (iconUrl != null);

            String posCls = useIcon ? "" : pos[idx % pos.length];
            String thumbClass = "row-thumb" + (useIcon ? " icon" : " " + posCls);
            String thumbStyle = "background-image:url('" + (useIcon ? iconUrl : (bgUrl==null?"":bgUrl)) + "')";

            return """
            <div class="row">
              <div class="%s" style="%s"></div>
              <div class="row-content">
                <div class="row-title">%s</div>
                <div class="row-body">%s</div>
              </div>
            </div>
            """.formatted(thumbClass, thumbStyle, head, body);
        }).collect(Collectors.joining("\n"));
    }

    /** 헤딩 문구에 맞춰 아이콘 매핑 */
    private String iconForHeading(String heading){
        if (heading == null) return null;
        String h = heading.replace(" ", "");

        // 요청사항:
        // - "누가 대상인가요?" → go.png (왼쪽 아이콘, 흰 배경에 꽉 차게)
        // - "조건 한눈에" → what.png
        // - "신청 유의사항" → go.png
        if (h.contains("누가대상") || h.contains("누가대상인가요") || h.contains("대상")){
            return DESIGN_BASE + "/who.png";
        }
        if (h.contains("조건한눈에") || h.contains("조건")){
            return DESIGN_BASE + "/what.png";
        }
        if (h.contains("신청유의사항") || h.contains("유의사항")){
            return DESIGN_BASE + "/go.png";
        }
        return null; // 그 외는 배경 이미지를 크롭해서 사용
    }

    /** 운영사별 하단 유의문구 */
    private String buildFineprintBlockByOperator(PosterConfig cfg){
        String op = cfg.getOperator()!=null ? pick(cfg.getOperator().getName(), "") : "";
        List<String> lines = new ArrayList<>();
        String key = op==null? "" : op;

        if (key.contains("부산은행")) {
            lines.add("실제 한도·금리는 심사에 따라 달라질 수 있습니다");
            lines.add("수수료·부대비용(인지세 등)은 약정서로 확인하세요");
            lines.add("문의: 부산은행 영업점·모바일 앱 고객센터");
        } else if (key.contains("IBK기업은행")) {
            lines.add("대출 취급 여부 및 조건은 IBK 심사 기준에 따릅니다");
            lines.add("중도상환수수료 등 부대비용은 상품설명서 참고");
            lines.add("문의: IBK 고객센터 1566-2566");
        } else if (key.contains("서민금융진흥원")) {
            lines.add("상세 조건과 일정은 공고문을 참조하세요");
            lines.add("기관 심사 및 예산 소진 시 조기 종료될 수 있습니다");
            lines.add("문의: 서민금융콜센터 국번없이 1397");
        } else if (key.contains("소상공인시장진흥공단")) {
            lines.add("정책자금은 요건 충족 시 신청 가능하며 사전 상담 필요");
            lines.add("예산 소진 시 조기 마감될 수 있습니다");
            lines.add("문의: 소상공인 정책자금 콜센터 1357");
        } else {
            String badge = cfg.getBadge()==null? "" : cfg.getBadge();
            String b = badge.replace(" ", "");
            if (b.contains("정부지원금") || b.contains("정책지원")) {
                lines.add("상세 조건과 일정은 공고문을 참조하세요");
                lines.add("기관 심사 및 예산 소진 시 조기 종료될 수 있습니다");
                lines.add("문의: 주관기관 콜센터 또는 누리집");
            } else {
                lines.add("실제 한도·금리는 심사에 따라 달라질 수 있습니다");
                lines.add("수수료·부대비용(인지세 등)은 약정서로 확인하세요");
                lines.add("문의: 고객센터 또는 영업점");
            }
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
        out = replaceFirstN(KEYWORDS2, out, "<span class='kw'>$0</span>", 5);
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

    private static String stripGuideWord(String title){
        if (title == null) return "";
        String t = title.trim();
        t = t.replaceAll("\\s*안내(?:문)?(?:입니다)?\\s*$", "");
        return t;
    }

    private String buildTaglineSmart(PosterConfig cfg, String operator, String title) {
        String sub = pick(cfg.getSubtitle(), "").trim();
        if (!sub.isBlank()) return sub;

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

    private String computeBgY(PosterConfig cfg){
        String bg = "24%";
        String badge = cfg.getBadge()==null? "" : cfg.getBadge();
        if (badge.contains("정부지원금") || badge.contains("정책지원")) bg = "22%";
        String op = cfg.getOperator()!=null ? cfg.getOperator().getName() : "";
        if (op==null) op="";
        if (op.contains("서민금융진흥원")) return "22%";
        if (op.contains("소상공인시장진흥공단")) return "24%";
        if (op.contains("신용보증재단")) return "24%";
        String[] banks = {"IBK기업은행","국민은행","신한은행","우리은행","하나은행","농협은행주식회사","토스뱅크","카카오뱅크","케이뱅크","한국산업은행","한국스탠다드차타드은행"};
        for (String b : banks) if (op.contains(b)) return "24%";
        return bg;
    }
    /** 운영사 이름으로 로고 파일명 매핑 (없으면 govern.png) */
    private String operatorLogoForName(String nameRaw){
        if (nameRaw == null) return "govern.png";
        String n = nameRaw.replace("주식회사","").replace("(주)","").replace("은행","").trim();

        // 키워드 → 파일명
        String[][] pairs = new String[][]{
                {"부산", "bnk.png"},
                {"경남", "bnk.png"},
                {"국민", "kb.png"},
                {"KB",   "kb.png"},
                {"신한", "shinhan.png"},
                {"하나", "hana.png"},                // 필요시 img-hana-symbol-2.png로 교체 가능
                {"우리", "woori.png"},
                {"IBK",  "ibk.png"},
                {"기업", "ibk.png"},
                {"NH",   "nh.png"},
                {"농협", "nh.png"},
                {"토스", "toss.png"},
                {"카카오", "kakao.png"},
                {"SC",   "sc.png"},
                {"스탠다드차타드", "sc.png"},
                {"산업", "sanup.png"},              // KDB 산업은행
                {"제주", "jeju.png"},
                {"광주", "gwangju.png"},
                {"전북", "jeonbook.png"},
                {"대구", "BK_DAEGU_Profile.png"},
                {"수협", "su.png"},
                {"케이뱅크", "k.png"},
                {"IM", "im.png"},
                {"플러스", "plus.png"},
                {"우체국", "woori.png"}             // 미지정 시 임시 매핑
        };

        for (String[] p : pairs){
            if (n.contains(p[0])) return p[1];
        }
        return "govern.png";
    }

}

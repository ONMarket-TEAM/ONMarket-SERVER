package com.onmarket.cardnews.service;

import com.onmarket.cardnews.dto.PosterConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class HtmlTemplateService {

    /** 디자인 아이콘/푸터 이미지 베이스 경로 */
    private static final String DESIGN_BASE = "https://onmarket-userfiles.s3.ap-northeast-2.amazonaws.com/design";

    /** 히어로(중간) 이미지 목록 — 랜덤 선택 */
    private static final String[] HERO_IMAGES = {
            "together1.png", "good.png", "market.png", "hae.png", "water.png", "happy.png"
    };
    /** 직전 선택값(같은 이미지 연속 방지) */
    private static volatile int LAST_HERO_IDX = -1;

    /** 본문 키워드 강조용(볼드 전환) */
    private static final Pattern KEYWORDS2 = Pattern.compile("지원|제외|기간|금리|한도");
    private static final Pattern PCT       = Pattern.compile("\\d+(?:\\.\\d+)?\\s*%");
    private static final Pattern AMOUNT    = Pattern.compile("(?:\\d{1,3}(?:,\\d{3})+|\\d+)(?:\\s*)(?:원|만원|억원|천원|만원대|억원대)");
    private static final Pattern AGE       = Pattern.compile("만\\s*\\d+\\s*세");
    private static final Pattern KEYWORDS  = Pattern.compile("금리|한도|KCB|신용평가|영업점|스마트폰|모바일|마이너스한도대출|부산은행");

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
      --title-offset: 18px;
      --bg-y: ${BG_Y};
    }
    body{
      width:1024px;height:1536px;
      font-family:Pretendard,-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,"Noto Sans KR","Apple SD Gothic Neo","Helvetica Neue",Arial,"Malgun Gothic",sans-serif;
      background: url('${BG_URL}') center/cover no-repeat;
      background-position: center var(--bg-y);
      color:#0b1324; display:flex; flex-direction:column; position:relative;
      padding-bottom:120px; /* footerbar와 겹침 방지 */
    }

    /* 상단 바 */
    .topbar{
      width:100%; max-width:980px; margin:22px auto 0; padding:0 24px;
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

    /* ONMARKET 로고 */
    .maker{ display:inline-flex; align-items:center; gap:8px; }
    .maker img{ height:60px; object-fit:contain; }

    /* ====== 상단 타이틀 & 설명 ====== */
    .hero-plain{
      width:100%; max-width:980px; margin: var(--title-offset) auto 0; padding:0 24px;
      text-align:center;
    }
    .title-plain{
      margin:0; font-weight:900; font-size:77px; line-height:1.05; letter-spacing:-1px;
      word-break:keep-all;
    }
    .title-plain .colored{ color:var(--brand); }
    .title-desc{
      margin:10px auto 0; max-width:900px; padding:0 8px;
      font-size:28px; line-height:1.45; font-weight:700; color:#1f2937; word-break:keep-all;
    }

    /* ====== 히어로 이미지: 투명 배경 위 이미지 ====== */
    .hero-image{
      position:relative;
      width:100%;
      max-width:980px;
      height:860px;               /* 크게 */
      margin:-20px auto 0;
      display:flex; align-items:center; justify-content:center;
      overflow:hidden;
      z-index:1;
    }
    .hero-image img.hero-img{
      width:92%; height:auto;
      object-fit:contain;
      background:transparent;     /* 배경 없음 */
    }

    /* ====== 2x2 그리드(4칸) ====== */
    .content-band{ margin-top:22px; width:100%; max-width:980px; align-self:center; }
    .rows{
      width:92%;
      display:grid;
      grid-template-columns: 1fr 1fr;
      gap:20px;
      justify-items:center;
      margin:0 auto;
    }
    .row{
      width:100%;
      display:grid; grid-template-columns: 140px 1fr; gap:12px;
      background:transparent; /* 행 자체 배경 없음 */
      padding:8px;
    }
    /* 신청기간 카드는 항상 오른쪽 아랫칸 */
    .row.period{ grid-column: 2; grid-row: 2; }

    /* 왼쪽 동그란 흰 배경 아이콘 */
    .row-thumb{
      height:110px; width:110px; border-radius:50%;
      background:rgba(255,255,255,0.92);
      display:flex; align-items:center; justify-content:center;
      box-shadow:0 6px 14px rgba(17,37,94,.10);
      overflow:hidden;
    }
    .row-thumb img{ width:60%; height:60%; object-fit:contain; }

    /* 오른쪽 텍스트: 배경 없이 글만 */
    .row-content{ padding:4px; background:none; }
    .row-title{ font-weight:900; font-size:30px; margin:4px 0 8px; color:#0b1324; }
    .row-body{ display:flex; flex-direction:column; gap:6px; }
    .row-body .line{ position:relative; padding-left:12px; font-size:22px; line-height:1.5; color:#0b1324; }
    .row-body .line::before{ content:""; position:absolute; left:0; top:0.85em; width:6px; height:6px; border-radius:50%; background:var(--brand); }

    /* ====== 약관: 4칸과 같은 폭 ====== */
    .fineprint{
      width:92%;
      margin:24px auto;
      padding:0;
    }
    .fineprint-inner{
      position:relative;
      background:rgba(255,255,255,0.9);
      border:1px solid rgba(17,37,94,.08);
      border-radius:16px;
      box-shadow:0 8px 18px rgba(17,37,94,.06);
      padding:16px 220px; /* 좌우 로고 공간 */
    }
    .fineprint-inner::before,
    .fineprint-inner::after{
      content:"";
      position:absolute;
      top:10px; bottom:10px; width:200px;
      background-repeat:no-repeat; background-size:contain; background-position:center; opacity:.9;
    }
    .fineprint-inner::before{ left:20px;  background-image:url('${DESIGN_BASE}/footer1.png'); }
    .fineprint-inner::after{  right:20px; background-image:url('${DESIGN_BASE}/footer2.png'); }
    .fineprint-text{ font-size:18px; line-height:1.55; color:#4b5563; word-break:keep-all; }

    /* ====== 바닥 운영기관 ====== */
    .footerbar{
      position:absolute; left:0; right:0; bottom:0;
      width:100%; height:96px;
      display:flex; align-items:center; justify-content:center;
      text-align:center;
    }
    .footerbar .operator{ display:inline-flex; align-items:center; gap:12px; }
    .footerbar .operator img{ height:40px; object-fit:contain; }
    .footerbar .operator span{ font-size:28px; font-weight:500; color:#0b1324; }
  </style>
</head>
<body>

  <div class="topbar">
    ${BADGE_BLOCK}
    ${MAKER_BLOCK}
  </div>

  <!-- 상단 제목 + 설명 -->
  ${HERO_BLOCK}

  <!-- 메인(히어로) 이미지: 투명 배경 위 이미지 -->
  <div class="hero-image">
    <img class="hero-img" src="${DESIGN_BASE}/${HERO_IMG}" alt=""/>
  </div>

  <!-- 2x2(4칸) 콘텐츠 -->
  <div class="content-band">
    <div class="rows">
      ${ROWS}
    </div>
  </div>

  <!-- 약관 -->
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

        // 제목(단어별 색 입히기)
        String rawTitle = pick(cfg.getTitle(), "");
        String titleColored = colorizeTitle(stripGuideWord(rawTitle));

        String badge    = esc(pick(cfg.getBadge(), ""));
        String operator = esc(cfg.getOperator()!=null ? pick(cfg.getOperator().getName(), "") : "");

        String badgeClass = "loan";
        String bLower = badge.replace(" ", "");
        if (bLower.contains("정부지원금") || bLower.contains("정책지원")) badgeClass = "support";
        String badgeBlock = badge.isBlank() ? "" : "<div class='badge " + badgeClass + "'>" + badge + "</div>";

        String makerBlock = "<div class='maker' aria-label='Made by ONMARKET'>" +
                "<img src='" + DESIGN_BASE + "/onmarket_logo.png' alt='ONMARKET'/>" +
                "</div>";

        // 설명(태그라인)
        String rawOperator = cfg.getOperator()!=null ? pick(cfg.getOperator().getName(), "") : "";
        String tagline = esc(buildTaglineSmart(cfg, rawOperator, rawTitle));
        String heroBlock = rawTitle.isBlank() ? "" :
                "<div class=\"hero-plain\">" +
                        "<h1 class=\"title-plain\">" + titleColored + "</h1>" +
                        (tagline.isBlank()? "" : "<p class=\"title-desc\">" + tagline + "</p>") +
                        "</div>";

        // 2x2 그리드용 ROW HTML 생성 (신청기간 카드 오른쪽 아래 고정)
        String rows = buildGridRows(cfg, bgUrl);

        String fineprintBlock = buildFineprintBlockByOperator(cfg);

        // 운영사 로고 + 텍스트
        String operatorLogo = operatorLogoForName(rawOperator);
        String footerBlock = operator.isBlank() ? "" :
                "<div class='operator'>" +
                        "<img src='" + DESIGN_BASE + "/" + operatorLogo + "' alt=''/>" +
                        "<span>" + operator + "</span></div>";

        String bgY = computeBgY(cfg);

        // ✅ 랜덤으로 히어로 이미지 선택(직전과 동일하면 한 번 더 섞기)
        int idx = ThreadLocalRandom.current().nextInt(HERO_IMAGES.length);
        if (HERO_IMAGES.length > 1 && idx == LAST_HERO_IDX) {
            idx = (idx + 1) % HERO_IMAGES.length;
        }
        LAST_HERO_IDX = idx;
        String heroImg = HERO_IMAGES[idx];

        return TEMPLATE
                .replace("${ACCENT}", accent)
                .replace("${BRAND}", brand)
                .replace("${BG_URL}", bgUrl == null ? "" : bgUrl)
                .replace("${BG_Y}", bgY)
                .replace("${TITLE}", esc(stripGuideWord(rawTitle)))
                .replace("${BADGE_BLOCK}", badgeBlock)
                .replace("${MAKER_BLOCK}", makerBlock)
                .replace("${HERO_BLOCK}", heroBlock)
                .replace("${ROWS}", rows)
                .replace("${FINEPRINT_BLOCK}", fineprintBlock)
                .replace("${FOOTER_BLOCK}", footerBlock)
                .replace("${DESIGN_BASE}", DESIGN_BASE)
                .replace("${HERO_IMG}", heroImg);
    }

    /* ====== 2x2 그리드 빌드: 신청기간을 항상 오른쪽 아래로 ====== */
    private String buildGridRows(PosterConfig cfg, String bgUrl){
        List<String> blocks = new ArrayList<>();

        // 고정 헤드
        String[] fixedHeads = {"신청 대상", "신청 조건", "유의사항"};

        List<PosterConfig.Section> sections = cfg.getSections();
        int secCount = (sections==null) ? 0 : Math.min(3, sections.size());
        for (int i=0;i<secCount;i++){
            PosterConfig.Section s = sections.get(i);
            String head = fixedHeads[i];

            String iconUrl = iconForHeading(head);
            String thumb = "<div class='row-thumb'><img src='" + (iconUrl!=null?iconUrl:(DESIGN_BASE + "/what.png")) + "' alt='icon'/></div>";

            String bodyHtml;
            if (s.getBullets()!=null && !s.getBullets().isEmpty()) {
                bodyHtml = s.getBullets().stream()
                        .map(b -> "<div class='line'>"+ emphasize(esc(pick(b,""))) +"</div>")
                        .collect(Collectors.joining());
            } else {
                bodyHtml = "<div class='line'>" + emphasize(esc(pick(s.getText(),""))) + "</div>";
            }

            String right = "<div class='row-content'><div class='row-title'>" + head + "</div><div class='row-body'>" + bodyHtml + "</div></div>";
            blocks.add("<div class='row'>" + thumb + right + "</div>");
        }

        // 신청 기간(오른쪽 아래)
        List<String> periodLines = (cfg.getApplyPeriodLines() != null) ? cfg.getApplyPeriodLines() : List.of();
        String body = periodLines.stream()
                .map(t -> "<div class='line'>" + emphasize(esc(t)) + "</div>")
                .collect(Collectors.joining());
        String left = "<div class='row-thumb'><img src='" + DESIGN_BASE + "/footer1.png' alt='period'/></div>";
        String right = "<div class='row-content'><div class='row-title'>신청 기간</div><div class='row-body'>" + body + "</div></div>";
        blocks.add("<div class='row period'>" + left + right + "</div>");

        return String.join("\n", blocks);
    }

    /** 타이틀 단어를 번갈아 색칠 */
    private String colorizeTitle(String title){
        if (title == null || title.isBlank()) return "";
        String[] parts = title.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        boolean colored = false;
        for (String w : parts){
            if (colored) sb.append("<span class='colored'>").append(esc(w)).append("</span>");
            else sb.append(esc(w));
            sb.append(" ");
            colored = !colored;
        }
        return sb.toString().trim();
    }

    /** 강조: 전부 bold로만 처리(색상 없음) */
    private static String emphasize(String escaped){
        if (escaped==null || escaped.isBlank()) return escaped;
        String out = escaped.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");
        out = replaceFirstN(PCT, out, "<b>$0</b>", 2);
        out = replaceFirstN(AMOUNT, out, "<b>$0</b>", 2);
        out = replaceFirstN(AGE, out, "<b>$0</b>", 1);
        out = replaceFirstN(KEYWORDS, out, "<b>$0</b>", 2);
        out = replaceFirstN(KEYWORDS2, out, "<b>$0</b>", 5);
        return out;
    }

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

    /* ====== 유틸 ====== */
    private static String pick(String v, String def){ return (v==null||v.isBlank())?def:v; }
    private static String esc(String s){
        if (s==null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#39;");
    }

    /** 팔레트(브랜드/액센트) 기본값 */
    private String[] fallbackPalette(PosterConfig cfg){
        String badge = cfg.getBadge()==null? "" : cfg.getBadge();
        String b = badge.replace(" ", "");
        if (b.contains("정부지원금") || b.contains("정책지원")) {
            return new String[]{"#10b981", "#3b82f6"}; // green / blue
        }
        return new String[]{"#2563eb", "#06b6d4"};     // blue / cyan
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
                    if (s.getBullets()!=null && !s.getBullets().isEmpty()) audience = sanitizeAudience(s.getBullets().get(0));
                    else if (s.getText()!=null) audience = sanitizeAudience(s.getText());
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

    /** 헤딩 문구에 맞춰 아이콘 매핑 */
    private String iconForHeading(String heading){
        if (heading == null) return null;
        String h = heading.replace(" ", "");
        if (h.contains("누가대상") || h.contains("누가대상인가요") || h.contains("대상")){
            return DESIGN_BASE + "/who.png";
        }
        if (h.contains("조건한눈에") || h.contains("조건")){
            return DESIGN_BASE + "/what.png";
        }
        if (h.contains("신청유의사항") || h.contains("유의사항")){
            return DESIGN_BASE + "/go.png";
        }
        return null;
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

    /** 운영사 이름으로 로고 파일명 매핑 (없으면 govern.png) */
    private String operatorLogoForName(String nameRaw){
        if (nameRaw == null) return "govern.png";
        String n = nameRaw.replace("주식회사","").replace("(주)","").replace("은행","").trim();

        String[][] pairs = new String[][]{
                {"부산", "bnk.png"},
                {"경남", "bnk.png"},
                {"국민", "kb.png"},
                {"KB",   "kb.png"},
                {"신한", "shinhan.png"},
                {"하나", "hana.png"},
                {"우리", "woori.png"},
                {"IBK",  "ibk.png"},
                {"기업", "ibk.png"},
                {"NH",   "nh.png"},
                {"농협", "nh.png"},
                {"토스", "toss.png"},
                {"카카오", "kakao.png"},
                {"SC",   "sc.png"},
                {"스탠다드차타드", "sc.png"},
                {"산업", "sanup.png"},
                {"제주", "jeju.png"},
                {"광주", "gwangju.png"},
                {"전북", "jeonbook.png"},
                {"대구", "BK_DAEGU_Profile.png"},
                {"수협", "su.png"},
                {"케이뱅크", "k.png"},
                {"IM", "im.png"},
                {"플러스", "plus.png"},
                {"우체국", "woori.png"}
        };

        for (String[] p : pairs){
            if (n.contains(p[0])) return p[1];
        }
        return "govern.png";
    }
}

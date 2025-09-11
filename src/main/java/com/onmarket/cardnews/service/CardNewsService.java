package com.onmarket.cardnews.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ScreenshotType;
import com.onmarket.cardnews.dto.PosterConfig;
import com.onmarket.cardnews.dto.TargetType;
import com.onmarket.common.openai.OpenAIClient;
import com.onmarket.fssdata.domain.CreditLoanProduct;
import com.onmarket.fssdata.repository.CreditLoanProductRepository;
import com.onmarket.loandata.domain.LoanProduct;
import com.onmarket.loandata.repository.LoanProductRepository;
import com.onmarket.supportsdata.domain.SupportProduct;
import com.onmarket.supportsdata.repository.SupportProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.onmarket.post.repository.PostRepository;
import org.springframework.beans.factory.annotation.Value;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardNewsService {

    private final OpenAIClient openAI;
    private final HtmlTemplateService htmlTemplate;
    private final S3Uploader s3Uploader;

    private final LoanProductRepository loanRepo;
    private final CreditLoanProductRepository creditRepo;
    private final SupportProductRepository supportRepo;

    private final ObjectMapper mapper = new ObjectMapper();

    private static final int WIDTH = 1024;
    private static final int HEIGHT = 1536;
    private final PostRepository postRepo;   // ✅ 추가

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;                  // ✅ 추가 (yml 없으면 기본값 localhost)
    /** 단순 팔레트 구조 */
    private record Palette(String brand, String accent, boolean strict) {}

    /** 운영사 → 팔레트 맵(하드코딩) */
    private static final Map<String, Palette> OP_PALETTES = new HashMap<>();
    static {
        // ... (팔레트 테이블은 기존 코드 그대로)
        // 생략
    }

    /** 이름 정규화 */
    private static String normalizeOperator(String s) {
        if (s == null) return "";
        String n = s.trim();
        n = n.replaceAll("[()\\s]", "");
        n = n.replaceAll("^(재단법인|재단|주식회사|\\(재\\)|\\(주\\))", "");
        n = n.replaceAll("(공사|공단|재단|협회|진흥원|재청|지원센터|센터)$", "");
        n = n.replaceAll("특별자치도", "");
        return n;
    }

    /** 팔레트 해석 */
    private static Optional<Palette> resolvePalette(String rawOperatorName) {
        if (rawOperatorName == null || rawOperatorName.isBlank()) return Optional.empty();
        String key = normalizeOperator(rawOperatorName);
        if (OP_PALETTES.containsKey(key)) return Optional.of(OP_PALETTES.get(key));
        Palette direct = OP_PALETTES.get(rawOperatorName.trim());
        if (direct != null) return Optional.of(direct);
        return OP_PALETTES.entrySet().stream()
                .filter(e -> key.contains(e.getKey()) || e.getKey().contains(key) || rawOperatorName.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    /* ===================== 메인 플로우 ===================== */

    @Transactional
    public String buildFromDbAndPersist(TargetType type, String idRaw) {
        String rawRow = switch (type) {
            case LOAN_PRODUCT        -> assembleLoan(Long.parseLong(idRaw));
            case CREDIT_LOAN_PRODUCT -> assembleCredit(Long.parseLong(idRaw));
            case SUPPORT_PRODUCT     -> assembleSupport(Long.parseLong(idRaw));
        };

        PosterConfig cfg = buildPosterConfig(rawRow);
        enforceOperatorPalette(cfg);
        ensureApplyPeriodLines(cfg, rawRow);

        String bgPrompt  = buildBackgroundPrompt(cfg);
        String bgDataUrl = openAI.imageDataUrl(bgPrompt, WIDTH, HEIGHT);

        byte[] png = render(cfg, bgDataUrl);

        String key = s3Uploader.uploadCardNews(png, UUID.randomUUID().toString());

        // ✅ 절대 URL(베이스 URL + 프록시 경로)
        String proxyUrl = baseUrl + "/api/cardnews/image?key=" + URLEncoder.encode(key, StandardCharsets.UTF_8);

        // ✅ Post.image_url 업데이트 (source_table + source_id 매칭)
        String sourceTable = mapSourceTable(type);              // "LoanProduct"/"CreditLoanProduct"/"SupportProduct"
        Long   sourceId    = Long.parseLong(idRaw);
        LocalDateTime now = LocalDateTime.now();

        int updated = postRepo.updateImageUrlBySource(sourceTable, sourceId, proxyUrl, now);
        if (updated == 0) {
            // 매칭되는 Post가 없을 때 로그만 남김 (신규 생성 정책이 있으면 여기에 추가)
            log.warn("Post not found to update image_url (source_table={}, source_id={})", sourceTable, sourceId);
        }

        // (선택) 기존 원본 테이블(Loan/Credit/Support)에도 저장하고 싶다면 아래 블록 유지/수정
        // 현재 요구사항이 'post.image_url' 갱신이라면 이 블록은 제거해도 됩니다.
    /*
    switch (type) {
        case LOAN_PRODUCT -> { ... }
        case CREDIT_LOAN_PRODUCT -> { ... }
        case SUPPORT_PRODUCT -> { ... }
    }
    */

        return proxyUrl;
    }

    /** TargetType → post.source_table 매핑 */
    private String mapSourceTable(TargetType type) {
        return switch (type) {
            case LOAN_PRODUCT        -> "LoanProduct";
            case CREDIT_LOAN_PRODUCT -> "CreditLoanProduct";
            case SUPPORT_PRODUCT     -> "SupportProduct";
        };
    }

    @Transactional(readOnly = true)
    public String getPresignedUrlForKey(String key) {
        return s3Uploader.generatePresignedUrl(key);
    }

    /* ===================== PosterConfig 생성 ===================== */

    /** 카드뉴스용 데이터 생성 프롬프트 — applyPeriodLines 강제 */
    private PosterConfig buildPosterConfig(String rawRow) {
        String system = """
너는 한국어 카드뉴스 카피라이터다. 반드시 JSON만 출력한다.

스키마(빈 문자열 금지, 불필요 키 금지):
{
  "title": "string",
  "subtitle": "string",
  "badge": "대출상품|정부지원금",
  "date": "YYYY.MM.DD",
  "operator": {
    "name": "string",
    "type": "은행|정부|지자체|공공기관|기업"
  },
  "theme": { "brand": "#RRGGBB", "accent": "#RRGGBB" },
  "sections": [
    { "heading": "누가 대상인가요?", "bullets": ["14~22자","14~22자","14~22자"] },
    { "heading": "조건 한눈에",     "bullets": ["14~22자","14~22자","14~22자"] },
    { "heading": "신청·유의사항",   "bullets": ["14~22자","14~22자","14~22자"] }
  ],
  "applyPeriodLines": [
    "신청: ...",  // 예: 신청: 2025.09.01 ~ 2025.09.30 또는 신청: 상시
    "심사: ...",  // 예: 심사: 서류 심사 순차 진행 / 심사: ~2025.10.10
    "결과: ..."   // 예: 결과: 2025.10.15 발표 / 결과: 개별 통지
  ]
}

규칙:
- 원문에 기간/접수/마감/발표일 등이 있으면 해당 수치를 그대로 반영.
- 없으면 '신청: 상시', '심사: 서류 심사 순차 진행', '결과: 개별 통지'로 채움.
- 숫자/기간 왜곡 금지, 과장/추측 금지.
- 출력은 JSON 한 덩어리. 설명/주석/코드블록 금지.
""";
        String user = "다음 원문을 카드뉴스 데이터로 구조화하세요:\n\n" + rawRow;

        try {
            String json = openAI.chatJson(system, user, 0.4);
            return mapper.readValue(json, PosterConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("PosterConfig 생성 실패", e);
        }
    }

    /** 운영사 팔레트 주입 */
    private void enforceOperatorPalette(PosterConfig cfg) {
        String opName = (cfg.getOperator() != null ? cfg.getOperator().getName() : null);
        Optional<Palette> opt = resolvePalette(opName);
        if (opt.isEmpty()) return;
        Palette p = opt.get();
        try {
            if (cfg.getTheme() == null) cfg.setTheme(new PosterConfig.Theme());
            cfg.getTheme().setBrand(p.brand());
            cfg.getTheme().setAccent(p.accent());
        } catch (Throwable t) {
            log.debug("PosterConfig 팔레트 주입 실패(무시): {}", t.getMessage());
        }
    }

    /* ===================== 신청/심사/결과 보정 ===================== */

    // 날짜 패턴 (YYYY.MM.DD / YYYY-MM-DD / YYYY/MM/DD)
    private static final Pattern DATE =
            Pattern.compile("(\\d{4}[.\\-/](?:0?[1-9]|1[0-2])[.\\-/](?:0?[1-9]|[12]\\d|3[01]))");

    // 날짜 구간 패턴: A ~ B
    private static final Pattern DATE_RANGE =
            Pattern.compile("(\\d{4}[.\\-/]\\d{1,2}[.\\-/]\\d{1,2})\\s*[~∼-]\\s*(\\d{4}[.\\-/]\\d{1,2}[.\\-/]\\d{1,2})");

    /** 원문과 기존 cfg를 바탕으로 applyPeriodLines를 “신청/심사/결과” 3줄로 확정 */
    private void ensureApplyPeriodLines(PosterConfig cfg, String rawRow) {
        List<String> lines = cfg.getApplyPeriodLines();
        if (lines == null) lines = new ArrayList<>();

        // 이미 “신청:” 형태가 있으면 그대로 두되, 3줄이 안 되면 보충
        boolean hasApply   = lines.stream().anyMatch(s -> s != null && s.trim().startsWith("신청:"));
        boolean hasReview  = lines.stream().anyMatch(s -> s != null && s.trim().startsWith("심사:"));
        boolean hasResult  = lines.stream().anyMatch(s -> s != null && s.trim().startsWith("결과:"));

        // 원문에서 날짜/키워드 단서 추출
        String applyText  = inferApply(rawRow).orElse("신청: 상시 신청 가능");
        String reviewText = inferReview(rawRow).orElse("심사: 서류 심사 순차 진행");
        String resultText = inferResult(rawRow).orElse("결과: 개별 통지");

        // 비었거나 레이블이 없는 경우 전부 재구성
        if (!hasApply && !hasReview && !hasResult) {
            lines = new ArrayList<>();
            lines.add(applyText);
            lines.add(reviewText);
            lines.add(resultText);
        } else {
            // 빠진 레이블만 보충
            if (!hasApply)  lines.add(0, applyText);
            if (!hasReview) lines.add(1, reviewText);
            if (!hasResult) lines.add(resultText);
            // 레이블 없는 기존 항목은 무시하거나 뒤에 둠 (필요시 정제 가능)
        }

        // 과도한 줄수 방지: 최대 3줄
        if (lines.size() > 3) {
            lines = lines.subList(0, 3);
        }

        cfg.setApplyPeriodLines(lines);
    }

    /** “신청:” 생성 */
    private Optional<String> inferApply(String raw) {
        if (raw == null) return Optional.empty();

        // 1) 구간 날짜(접수/신청/모집 포함)
        if (containsAny(raw, "접수", "신청", "모집", "기간")) {
            Matcher m = DATE_RANGE.matcher(raw);
            if (m.find()) {
                return Optional.of("신청: " + normalizeDate(m.group(1)) + " ~ " + normalizeDate(m.group(2)));
            }
            // "마감" + 단일일자
            if (raw.contains("마감")) {
                Optional<String> d = findNearbyDate(raw, "마감");
                if (d.isPresent()) return Optional.of("신청: ~ " + d.get() + " 마감");
            }
            // "부터"/"까지" 단서
            if (raw.contains("부터") || raw.contains("까지")) {
                List<String> two = findTwoDates(raw);
                if (two.size() == 2) {
                    return Optional.of("신청: " + two.get(0) + " ~ " + two.get(1));
                }
            }
        }

        // 2) 상시
        if (raw.contains("상시") || raw.contains("수시")) {
            return Optional.of("신청: 상시 신청 가능");
        }

        // 3) 단일 일자라도 발견되면 ‘부터’ 뉘앙스
        Matcher m = DATE.matcher(raw);
        if (m.find()) {
            return Optional.of("신청: " + normalizeDate(m.group(1)) + "부터");
        }

        return Optional.empty();
    }

    /** “심사:” 생성 */
    private Optional<String> inferReview(String raw) {
        if (raw == null) return Optional.empty();
        if (containsAny(raw, "심사", "검토", "평가")) {
            Optional<String> d2 = findNearbyDate(raw, "심사");
            if (d2.isPresent()) return Optional.of("심사: " + d2.get() + "까지");
            return Optional.of("심사: 서류 심사 순차 진행");
        }
        return Optional.empty();
    }

    /** “결과:” 생성 (발표/통지/안내) */
    private Optional<String> inferResult(String raw) {
        if (raw == null) return Optional.empty();
        if (containsAny(raw, "결과", "발표", "통지", "안내")) {
            Optional<String> d3 = findNearbyDate(raw, "발표");
            if (d3.isEmpty()) d3 = findNearbyDate(raw, "결과");
            if (d3.isPresent()) return Optional.of("결과: " + d3.get() + " 발표");
            return Optional.of("결과: 개별 통지");
        }
        return Optional.empty();
    }

    private boolean containsAny(String s, String... ks){
        for (String k: ks) if (s.contains(k)) return true;
        return false;
    }

    /** 특정 키워드 주변(±60자)에서 첫 날짜 추출 */
    private Optional<String> findNearbyDate(String raw, String keyword){
        int idx = raw.indexOf(keyword);
        if (idx < 0) return Optional.empty();
        int start = Math.max(0, idx - 60);
        int end   = Math.min(raw.length(), idx + 60);
        String window = raw.substring(start, end);
        Matcher m = DATE.matcher(window);
        if (m.find()) return Optional.of(normalizeDate(m.group(1)));
        return Optional.empty();
    }

    /** 본문 전체에서 두 개의 날짜를 찾아 반환 (처음 2개) */
    private List<String> findTwoDates(String raw){
        List<String> out = new ArrayList<>();
        Matcher m = DATE.matcher(raw);
        while (m.find() && out.size() < 2) out.add(normalizeDate(m.group(1)));
        return out;
    }

    /** YYYY.MM.DD 형태로 정규화 */
    private String normalizeDate(String s){
        String t = s.replace('-', '.').replace('/', '.');
        String[] p = t.split("\\.");
        if (p.length >= 3) {
            String yyyy = p[0];
            String mm = p[1].length()==1 ? "0"+p[1] : p[1];
            String dd = p[2].length()==1 ? "0"+p[2] : p[2];
            return yyyy + "." + mm + "." + dd;
        }
        return t;
    }

    /** DALLE 배경 프롬프트 (사람/캐릭터 금지, 은은한 아파트/풍경 전용) */
    private String buildBackgroundPrompt(PosterConfig cfg) {
        String badge = safe(cfg.getBadge());
        String operatorName = (cfg.getOperator() != null ? safe(cfg.getOperator().getName()) : "");

        Optional<Palette> op = resolvePalette(operatorName);
        String brand = op.map(Palette::brand)
                .orElseGet(() -> (cfg.getTheme()!=null && cfg.getTheme().getBrand()!=null)
                        ? cfg.getTheme().getBrand() : "#7C3AED");
        String accent = op.map(Palette::accent)
                .orElseGet(() -> (cfg.getTheme()!=null && cfg.getTheme().getAccent()!=null)
                        ? cfg.getTheme().getAccent() : "#14B8A6");

        return """
Create a single poster BACKGROUND image at 1024x1536, PNG.

Goal:
- Make a very subtle, abstract-feeling BACKGROUND that evokes
  apartment skyline, misty cityscape, or soft watercolor landscape.
- It should look like a hazy backdrop, not a main subject.
- Absolutely NO people, characters, or mascots.

Hard constraints:
- No strong focal objects. All scenery should appear distant, blurred, low-contrast.
- Avoid emojis, icons, clipart, or readable text.
- Use pastel tones only (soft, light, gentle). Avoid saturated/neon colors.
- No gradients — instead, use a single soft pastel base wash with blurred scenery.
- Think of it like a faded photo texture or misty watercolor background.

Composition & layout:
- TOP 0–14%: keep completely EMPTY (reserved for HTML title).
- From 16% downward: apply the blurred scenery subtly, like faint silhouettes.
- Central band (~36–60% Y): keep extra calm and clean for text legibility.
- Everything should feel “background only,” almost invisible unless looked at closely.

Color palette hint:
- Base: a single pastel wash.
- Primary accent: {BRAND}, secondary accent: {ACCENT}, both applied only as faint, low-opacity tones.
- Ensure the whole atmosphere feels serene and unobtrusive.

Category: {BADGE}. Operator: {OPERATOR}.
""".replace("{BADGE}", badge == null ? "" : badge)
                .replace("{OPERATOR}", operatorName)
                .replace("{BRAND}", brand)
                .replace("{ACCENT}", accent);
    }

    private byte[] render(PosterConfig cfg, String bgDataUrl) {
        String html = htmlTemplate.renderHtml(cfg, bgDataUrl);
        try (Playwright pw = Playwright.create()) {
            Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext ctx = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(WIDTH, HEIGHT)
                    .setDeviceScaleFactor(2));
            Page page = ctx.newPage();
            page.setContent(html);
            byte[] png = page.screenshot(new Page.ScreenshotOptions().setType(ScreenshotType.PNG));
            ctx.close(); browser.close();
            return png;
        }
    }

    /* ===================== 데이터 조립 ===================== */

    private String assembleLoan(Long id){
        LoanProduct p = loanRepo.findById(id).orElseThrow();
        StringBuilder sb = new StringBuilder();
        sb.append("배지: 대출상품\n");
        sb.append("상품명: ").append(n(p.getProductName())).append("\n");
        sb.append("용도: ").append(n(p.getUsage())).append("\n");
        sb.append("대상: ").append(n(p.getTarget())).append("\n");
        sb.append("제공기관: ").append(n(p.getOfferingInstitution())).append("\n");
        if (p.getInterestCategory()!=null || p.getInterestRate()!=null)
            sb.append("금리: ").append(n(p.getInterestCategory())).append(" / ").append(n(p.getInterestRate())).append("\n");
        if (p.getLoanLimit()!=null) sb.append("한도: ").append(n(p.getLoanLimit())).append("\n");
        if (p.getMaxTotalTerm()!=null) sb.append("기간: ").append(n(p.getMaxTotalTerm())).append("개월\n");
        if (p.getRepaymentMethod()!=null) sb.append("상환방식: ").append(p.getRepaymentMethod()).append("\n");
        if (p.getAge()!=null) sb.append("연령조건: ").append(p.getAge()).append("\n");
        if (p.getSpecialTargetConditions()!=null) sb.append("특별대상: ").append(p.getSpecialTargetConditions()).append("\n");
        return sb.toString();
    }

    private String assembleCredit(Long id){
        CreditLoanProduct cp = creditRepo.findById(id).orElseThrow();
        StringBuilder sb = new StringBuilder();
        sb.append("배지: 대출상품\n");
        sb.append("상품명: ").append(n(cp.getFinPrdtNm())).append("\n");
        sb.append("기관명: ").append(n(cp.getKorCoNm())).append("\n");
        if (cp.getCrdtPrdtTypeNm()!=null) sb.append("상품종류: ").append(cp.getCrdtPrdtTypeNm()).append("\n");
        if (cp.getJoinWay()!=null) sb.append("가입방법: ").append(cp.getJoinWay()).append("\n");
        if (cp.getCbName()!=null) sb.append("신용평가기관: ").append(cp.getCbName()).append("\n");
        cp.getOptions().stream().filter(o -> o.getCrdtGradAvg()!=null).findFirst()
                .ifPresent(o -> sb.append("평균 금리: ").append(trimZero(o.getCrdtGradAvg())).append("%\n"));
        return sb.toString();
    }

    private String assembleSupport(Long id){
        SupportProduct sp = supportRepo.findById(id).orElseThrow();
        StringBuilder sb = new StringBuilder();
        sb.append("배지: 정부지원금\n");
        sb.append("정책명: ").append(n(sp.getServiceName())).append("\n");
        sb.append("주관기관: ").append(n(sp.getDepartmentName())).append("\n");
        if (sp.getSupportTarget()!=null) sb.append("지원대상: ").append(sp.getSupportTarget()).append("\n");
        if (sp.getSupportContent()!=null) sb.append("지원내용: ").append(sp.getSupportContent()).append("\n");
        if (sp.getApplicationMethod()!=null) sb.append("신청방법: ").append(sp.getApplicationMethod()).append("\n");
        if (sp.getDisplayDeadline()!=null) sb.append("마감일: ").append(sp.getDisplayDeadline()).append("\n");
        return sb.toString();
    }

    /* ===================== 유틸 ===================== */

    private String safe(String s){ return s==null?"":s; }
    private String n(String s){ return s==null?"":s.trim(); }
    private String trimZero(Double d){ String s=String.valueOf(d); return s.endsWith(".0")?s.substring(0,s.length()-2):s; }
}
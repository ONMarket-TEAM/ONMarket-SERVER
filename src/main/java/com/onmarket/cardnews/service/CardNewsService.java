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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * 카드뉴스 생성 서비스 (운영사별 팔레트 하드코딩 적용)
 */
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

    /* ===================== 팔레트 하드코딩 ===================== */

    /** 단순 팔레트 구조 */
    private record Palette(String brand, String accent, boolean strict) {}

    /** 운영사 → 팔레트 맵(하드코딩). 필요시 아래에 추가하세요. */
    private static final Map<String, Palette> OP_PALETTES = new HashMap<>();
    static {
        // 공공/유관
        OP_PALETTES.put("서민금융진흥원", new Palette("#0FA9AB", "#F59E0B", true));
        OP_PALETTES.put("소상공인시장진흥공단", new Palette("#2563EB", "#10B981", false));
        OP_PALETTES.put("신용보증기금", new Palette("#1C8B3D", "#0EA5A5", true));
        OP_PALETTES.put("중소벤처기업부", new Palette("#0EA5E9", "#F97316", false));
        OP_PALETTES.put("한국전력공사", new Palette("#E11D48", "#0EA5E9", false));

        // 시/도/군 등 지자체(공통 톤) — 필요시 지역별로 덮어쓰기
        OP_PALETTES.put("서울특별시", new Palette("#2563EB", "#0EA5E9", false));
        OP_PALETTES.put("부산광역시", new Palette("#0EA5E9", "#22C55E", false));
        OP_PALETTES.put("대구광역시", new Palette("#9333EA", "#10B981", false));
        OP_PALETTES.put("인천광역시", new Palette("#0284C7", "#22C55E", false));
        OP_PALETTES.put("광주광역시", new Palette("#16A34A", "#0EA5E9", false));
        OP_PALETTES.put("대전광역시", new Palette("#2563EB", "#F59E0B", false));
        OP_PALETTES.put("울산광역시", new Palette("#0891B2", "#F59E0B", false));
        OP_PALETTES.put("세종특별자치시", new Palette("#10B981", "#3B82F6", false));
        OP_PALETTES.put("경기도", new Palette("#2563EB", "#22C55E", false));
        OP_PALETTES.put("강원특별자치도", new Palette("#0EA5E9", "#22C55E", false));
        OP_PALETTES.put("충청북도", new Palette("#16A34A", "#0EA5E9", false));
        OP_PALETTES.put("충청남도", new Palette("#22C55E", "#0EA5E9", false));
        OP_PALETTES.put("전라북도", new Palette("#22C55E", "#0EA5E9", false));
        OP_PALETTES.put("전라남도", new Palette("#16A34A", "#3B82F6", false));
        OP_PALETTES.put("경상북도", new Palette("#2563EB", "#16A34A", false));
        OP_PALETTES.put("경상남도", new Palette("#22C55E", "#2563EB", false));
        OP_PALETTES.put("제주특별자치도", new Palette("#14B8A6", "#F59E0B", false));

        // 재단/공단 공통 톤(없으면 여기에 흡수)
        Arrays.asList("서울신용보증재단","경기신용보증재단","울산신용보증재단","전북신용보증재단","충북신용보증재단",
                        "전남신용보증재단","광주신용보증재단","인천신용보증재단","강원신용보증재단","부산신용보증재단",
                        "경북신용보증재단","충남신용보증재단","제주신용보증재단","대구신용보증재단")
                .forEach(n -> OP_PALETTES.put(n, new Palette("#16A34A", "#10B981", false)));

        // 은행/금융
        OP_PALETTES.put("IBK기업은행", new Palette("#0067AC", "#00AEEF", true));
        OP_PALETTES.put("국민은행", new Palette("#FFC20E", "#3A3A3A", true));
        OP_PALETTES.put("신한은행", new Palette("#0E5AA7", "#7CB4E0", true));
        OP_PALETTES.put("우리은행", new Palette("#0067AC", "#00AEEF", false));
        OP_PALETTES.put("하나은행", new Palette("#00857C", "#38BDF8", false));
        OP_PALETTES.put("농협은행주식회사", new Palette("#0078C1", "#FFD400", false));
        OP_PALETTES.put("주식회사 카카오뱅크", new Palette("#FEE500", "#111111", true));
        OP_PALETTES.put("주식회사 케이뱅크", new Palette("#E4007F", "#111111", false));
        OP_PALETTES.put("토스뱅크 주식회사", new Palette("#1B64DA", "#60A5FA", false));
        OP_PALETTES.put("한국산업은행", new Palette("#005EB8", "#00A6D6", false));
        OP_PALETTES.put("한국스탠다드차타드은행", new Palette("#00A885", "#004D3F", false));
    }

    /** 이름 정규화(괄호/공백/법인형태/공단/공사/재단/은행 등 접미 제거) */
    private static String normalizeOperator(String s) {
        if (s == null) return "";
        String n = s.trim();
        n = n.replaceAll("[()\\s]", "");
        n = n.replaceAll("^(재단법인|재단|주식회사|\\(재\\)|\\(주\\))", "");
        n = n.replaceAll("(공사|공단|재단|협회|진흥원|재청|지원센터|센터)$", "");
        n = n.replaceAll("특별자치도", ""); // 지역 표기 단순화
        return n;
    }

    /** 팔레트 해석 (완전일치 → 포함일치 순) */
    private static Optional<Palette> resolvePalette(String rawOperatorName) {
        if (rawOperatorName == null || rawOperatorName.isBlank()) return Optional.empty();
        String key = normalizeOperator(rawOperatorName);

        // 1) 정확 일치(정규화된 키)
        if (OP_PALETTES.containsKey(key)) return Optional.of(OP_PALETTES.get(key));

        // 2) 원본 이름으로도 한번(일부 키가 정규화 전 형태일 수 있음)
        Palette direct = OP_PALETTES.get(rawOperatorName.trim());
        if (direct != null) return Optional.of(direct);

        // 3) 부분 매칭 양방향
        return OP_PALETTES.entrySet().stream()
                .filter(e -> key.contains(e.getKey()) || e.getKey().contains(key)
                        || rawOperatorName.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    /* ===================== 메인 플로우 ===================== */

    /** DB에서 꺼내 자동 생성 + 업로드 + 해당 Row에 Key/URL 갱신 */
    @Transactional
    public String buildFromDbAndPersist(TargetType type, String idRaw) {
        String rawRow = switch (type) {
            case LOAN_PRODUCT        -> assembleLoan(Long.parseLong(idRaw));
            case CREDIT_LOAN_PRODUCT -> assembleCredit(Long.parseLong(idRaw));
            case SUPPORT_PRODUCT     -> assembleSupport(Long.parseLong(idRaw));
        };

        PosterConfig cfg = buildPosterConfig(rawRow);    // (1) JSON 생성
        enforceOperatorPalette(cfg);                     // (2) 운영사 팔레트 강제 주입(가능하면)

        String bgPrompt = buildBackgroundPrompt(cfg);    // (3) DALLE 배경 프롬프트
        String bgDataUrl = openAI.imageDataUrl(bgPrompt, WIDTH, HEIGHT);

        byte[] png = render(cfg, bgDataUrl);             // (4) HTML 렌더링 + 스크린샷

        String key = s3Uploader.uploadCardNews(png, UUID.randomUUID().toString());
        String proxyUrl = "/api/cardnews/image?key=" + URLEncoder.encode(key, StandardCharsets.UTF_8);

        Instant now = Instant.now();
        switch (type) {
            case LOAN_PRODUCT -> {
                LoanProduct lp = loanRepo.findById(Long.parseLong(idRaw))
                        .orElseThrow(() -> new RuntimeException("LoanProduct not found: " + idRaw));
                lp.updateCardnews(key, proxyUrl, now);
                loanRepo.save(lp);
            }
            case CREDIT_LOAN_PRODUCT -> {
                CreditLoanProduct cp = creditRepo.findById(Long.parseLong(idRaw))
                        .orElseThrow(() -> new RuntimeException("CreditLoanProduct not found: " + idRaw));
                cp.updateCardnews(key, proxyUrl, now);
                creditRepo.save(cp);
            }
            case SUPPORT_PRODUCT -> {
                SupportProduct sp = supportRepo.findById(Long.parseLong(idRaw))
                        .orElseThrow(() -> new RuntimeException("SupportProduct not found: " + idRaw));
                sp.updateCardnews(key, proxyUrl, now);
                supportRepo.save(sp);
            }
        }
        return proxyUrl;
    }

    @Transactional(readOnly = true)
    public String getPresignedUrlForKey(String key) {
        return s3Uploader.generatePresignedUrl(key);
    }

    /* ===================== PosterConfig 생성 ===================== */

    /** 카드뉴스용 데이터 생성 프롬프트 (테마색 포함) */
    private PosterConfig buildPosterConfig(String rawRow) {
        String system = """
너는 한국어 카드뉴스 카피라이터다. 반드시 JSON만 출력한다.

스키마(빈 문자열 금지, 불필요 키 금지):
{
  "title": "string",                // 6~12자, 줄바꿈/따옴표/이모지/괄호 금지
  "subtitle": "string",             // 12~24자, 1문장, 마침표 생략
  "badge": "대출상품|정부지원금",       // 두 가지 중 하나만
  "date": "YYYY.MM.DD",             // 원문에 기간/공고일 있으면 기입, 없으면 필드 생략
  "operator": {
    "name": "string",               // 은행/부처/기관명
    "type": "은행|정부|지자체|공공기관|기업"
  },
  "theme": {                        // 카드뉴스 전체 테마 팔레트
    "brand": "#RRGGBB",             // 주색(헤더/소제목/버튼)
    "accent": "#RRGGBB"             // 보조색(그라디언트/포인트)
  },
  "sections": [
    { "heading": "누가 대상인가요?", "bullets": ["14~22자","14~22자","14~22자"] },
    { "heading": "조건 한눈에",     "bullets": ["14~22자","14~22자","14~22자"] },
    { "heading": "신청·유의사항",   "bullets": ["14~22자","14~22자","14~22자"] }
  ]
}

규칙:
- 수치/기간/금리는 원문 그대로. 과장/추측/미확정 표현 금지.
- heading은 위 3개 문구만 사용.
- bullets는 핵심만 요약, 특수문자/이모지 금지, 각 14~22자.
- badge가 "대출상품"이면 선호 팔레트 예시: brand #2563EB, accent #06B6D4.
- badge가 "정부지원금"이면 선호 팔레트 예시: brand #10B981, accent #3B82F6.
- 단, '예시 색상'은 그대로 복사하지 말고 원문 맥락과 운영사 톤에 맞게 자체 팔레트를 제안.
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

    /** 운영사 팔레트를 PosterConfig.theme에 주입 (세터가 없으면 조용히 건너뜀) */
    private void enforceOperatorPalette(PosterConfig cfg) {
        String opName = (cfg.getOperator() != null ? cfg.getOperator().getName() : null);
        Optional<Palette> opt = resolvePalette(opName);
        if (opt.isEmpty()) return;

        Palette p = opt.get();
        try {
            if (cfg.getTheme() == null) {
                cfg.setTheme(new PosterConfig.Theme());
            }
            if (cfg.getTheme() != null) {
                cfg.getTheme().setBrand(p.brand());
                cfg.getTheme().setAccent(p.accent());
            }
        } catch (Throwable t) {
            // 세터/기본생성자가 없을 수 있으므로 실패해도 무시하고 배경 프롬프트에서만 강제
            log.debug("PosterConfig에 팔레트 주입 실패(무시): {}", t.getMessage());
        }
    }/** DALLE 배경 프롬프트 (소상공인 느낌 + 파스텔톤, 그라데이션 금지) */
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

        String themeHint = (badge != null && badge.contains("정부"))
                ? "Korean government support for small businesses"
                : "Korean small business banking and credit loan";

        String horizRule = Math.random() < 0.5
                ? "- Place the main illustration on the LEFT third (x≈28%).\n"
                : "- Place the main illustration on the RIGHT third (x≈72%).\n";

        return """
Create a single poster BACKGROUND image at 1024x1536, PNG.

Theme: {THEME_HINT}.
Visual style:
- Evoke small business owners: shop fronts, market stalls, calculators, loan documents, people consulting at a desk.
- Use pastel tones only (soft, light, gentle colors). DO NOT use saturated or neon colors.
- Absolutely NO gradients. Use solid pastel background colors with subtle flat shading.
- Add faint, almost invisible background motifs (like shop outlines, store shelves, simple icons) so it feels lively but subtle.
- Make it cute and friendly, not corporate.

Layout rules (Y axis in % of height):
- TOP 0–14%: leave completely EMPTY (reserved for HTML title).
- HERO BANNER 18–34%: put ONE figurative subject here (people with documents, shop owner, small shop, money sign).
  Keep inside a 980x240px panel centered at y≈26%.
- TEXT AREA 36–100%: only subtle abstract pastel patterns, no strong objects.

Placement rules:
{HORIZ_RULE}
- Do NOT center the subject. Keep bias to one side (left or right).
- No logos, no readable text.

Color palette hint:
- Primary color {BRAND}, secondary color {ACCENT}, but all rendered in pastel tones.
- Background should be solid pastel, not gradient.

Category: {BADGE}. Operator: {OPERATOR}.
""".replace("{THEME_HINT}", themeHint)
                .replace("{BRAND}", brand)
                .replace("{ACCENT}", accent)
                .replace("{BADGE}", badge == null ? "" : badge)
                .replace("{OPERATOR}", operatorName)
                .replace("{HORIZ_RULE}", horizRule);
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
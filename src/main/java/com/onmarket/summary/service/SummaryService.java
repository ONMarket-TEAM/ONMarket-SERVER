package com.onmarket.summary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onmarket.common.openai.OpenAIClient;
import com.onmarket.fssdata.domain.CreditLoanOption;
import com.onmarket.fssdata.domain.CreditLoanProduct;
import com.onmarket.fssdata.repository.CreditLoanOptionRepository;
import com.onmarket.fssdata.repository.CreditLoanProductRepository;
import com.onmarket.loandata.domain.LoanProduct;
import com.onmarket.loandata.repository.LoanProductRepository;
import com.onmarket.supportsdata.domain.SupportProduct;
import com.onmarket.supportsdata.repository.SupportProductRepository;
import com.onmarket.post.repository.PostRepository; // ✅ 추가
import com.onmarket.summary.dto.SummaryAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final OpenAIClient ai;
    private final ObjectMapper om = new ObjectMapper();

    private final LoanProductRepository loanRepo;
    private final CreditLoanProductRepository creditRepo;
    private final CreditLoanOptionRepository optionRepo;
    private final SupportProductRepository supportRepo;

    private final PostRepository postRepo; // ✅ 추가

    private static final String SYSTEM = """
        너는 한국의 금융/정부지원 상품을 소개하는 카피라이터야.
        주어진 원문 정보를 바탕으로 두 가지 요약을 JSON으로 출력해:
        
        {
          "short": "<누구에게 어떤 특징으로 제공되는 무슨 상품인지 한 문장. 28~60자.>",
          "long": "<2~3문단. 조건/절차/주의사항 등을 구체적으로.>"
        }
        규칙:
        - short는 카드형 요약용, long은 상세페이지용.
        - 이모지/과장/유보적 문구 금지.
        - 출력은 반드시 JSON 하나만.
        """;

    // 간단한 홀더
    private record Pair(String s, String l) {}

    /** LoanProduct → Post만 업데이트 */
    @Transactional
    public void generateForLoan(long id) {
        LoanProduct p = loanRepo.findById(id).orElseThrow();     // 프롬프트용 조회
        String user = SummaryAssembler.forLoan(p);
        String json = ai.chatJson(SYSTEM, user, 0.2);

        Pair pair = parseJson(json); // ✅ 엔티티 세팅/저장 안 함
        postRepo.updateSummaryBySource("LoanProduct", p.getId(), pair.s(), pair.l());
    }

    /** CreditLoanProduct → Post만 업데이트 */
    @Transactional
    public void generateForCredit(long id) {
        CreditLoanProduct p = creditRepo.findById(id).orElseThrow();
        List<CreditLoanOption> opts = optionRepo.findByFinPrdtCd(p.getFinPrdtCd());
        String user = SummaryAssembler.forCredit(p, opts);
        String json = ai.chatJson(SYSTEM, user, 0.2);

        Pair pair = parseJson(json);
        postRepo.updateSummaryBySource("CreditLoanProduct", p.getId(), pair.s(), pair.l());
    }

    /** SupportProduct → Post만 업데이트 */
    @Transactional
    public void generateForSupport(long serviceId) {
        SupportProduct s = supportRepo.findById(serviceId).orElseThrow();
        String user = SummaryAssembler.forSupport(s);
        String json = ai.chatJson(SYSTEM, user, 0.2);

        Pair pair = parseJson(json);
        // ⚠️ PK 접근자 이름 확인: getId()/getServiceId() 중 프로젝트에 맞게 사용
        postRepo.updateSummaryBySource("SupportProduct", s.getId(), pair.s(), pair.l());
    }

    // ---------- 공통 유틸 ----------

    /** JSON에서 short/long만 파싱 (엔티티 세팅 X) */
    private Pair parseJson(String json) {
        try {
            JsonNode root = om.readTree(json);
            String s = root.path("short").asText();
            String l = root.path("long").asText();
            if (s != null && s.length() > 140) s = s.substring(0, 140); // 필요 없으면 제거
            return new Pair(s, l);
        } catch (Exception e) {
            throw new RuntimeException("요약 파싱 실패: " + e.getMessage(), e);
        }
    }
}
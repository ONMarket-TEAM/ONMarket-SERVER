package com.onmarket.summary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onmarket.common.openai.OpenAIClient;
import com.onmarket.fssdata.domain.CreditLoanOption;
import com.onmarket.fssdata.repository.CreditLoanOptionRepository;
import com.onmarket.fssdata.repository.CreditLoanProductRepository;
import com.onmarket.loandata.repository.LoanProductRepository;
import com.onmarket.summary.dto.SummaryAssembler;
import com.onmarket.supportsdata.repository.SupportProductRepository;
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

    /** 카드형(short) + 상세(long) 생성 지침 (가벼운 톤 + 구체성) */
    private static final String SYSTEM = """
        너는 한국의 금융/정부지원 상품을 소개하는 카피라이터야.
        주어진 원문 정보를 바탕으로 두 가지 요약을 JSON으로 출력해:
        
        {
          "short": "<누구에게 어떤 특징으로 제공되는 무슨 상품인지 한 문장. 28~60자.
                    예: '서울 청년 예비창업자에게 저금리로 지원하는 창업자금 대출'>",
          "long": "<2~3문단. 첫 문단은 한 줄 요약, 이후 문단에서는 대상, 조건, 금리/한도/기간,
                   신청 방법, 주의사항 등을 구체적으로 적되 너무 길게 늘이지 말 것.
                   마침표로 끝내고 광고/과장/이모지 없이 깔끔하게 작성>"
        }
        
        규칙:
        - short는 카드형 요약용 → 가볍고 직관적으로.
        - long은 상세페이지용 → 실제 이용자가 참고할 수 있도록 조건과 절차를 구체적으로.
        - 금지: 이모지, 특수문자 남용, 허위·과장 표현, '자세한 내용은' 같은 유보적 문구.
        - 출력은 반드시 JSON 하나만.
        """;

    /** 단건 생성: loan_product */
    @Transactional
    public void generateForLoan(long id) {
        var p = loanRepo.findById(id).orElseThrow();
        String user = SummaryAssembler.forLoan(p);
        String json = ai.chatJson(SYSTEM, user, 0.2);
        applyJson(p::setSummaryShort, p::setSummaryLong, json);
        loanRepo.save(p);
    }

    /** 단건 생성: credit_loan_product */
    @Transactional
    public void generateForCredit(long id) {
        var p = creditRepo.findById(id).orElseThrow();
        List<CreditLoanOption> opts = optionRepo.findByFinPrdtCd(p.getFinPrdtCd());
        String user = SummaryAssembler.forCredit(p, opts);
        String json = ai.chatJson(SYSTEM, user, 0.2);
        applyJson(p::setSummaryShort, p::setSummaryLong, json);
        creditRepo.save(p);
    }

    /** 단건 생성: supportservice/support_product */
    @Transactional
    public void generateForSupport(long serviceId) {
        var s = supportRepo.findById(serviceId).orElseThrow();
        String user = SummaryAssembler.forSupport(s);
        String json = ai.chatJson(SYSTEM, user, 0.2);
        applyJson(s::setSummaryShort, s::setSummaryLong, json);
        supportRepo.save(s);
    }
    // ---------- 공통 유틸 ----------

    private interface Setter { void set(String v); }

    private void applyJson(Setter shortSetter, Setter longSetter, String json) {
        try {
            JsonNode root = om.readTree(json);
            String s = root.path("short").asText();
            String l = root.path("long").asText();
            if (s != null && s.length() > 140) s = s.substring(0, 140); // 안전장치
            shortSetter.set(s);
            longSetter.set(l);
        } catch (Exception e) {
            throw new RuntimeException("요약 파싱 실패: " + e.getMessage(), e);
        }
    }

    private boolean isBlank(String x) { return x == null || x.isBlank(); }
}
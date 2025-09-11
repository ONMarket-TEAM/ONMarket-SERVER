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
import com.onmarket.post.domain.Post;
import com.onmarket.post.exception.PostNotFoundException;
import com.onmarket.supportsdata.domain.SupportProduct;
import com.onmarket.supportsdata.repository.SupportProductRepository;
import com.onmarket.post.repository.PostRepository;
import com.onmarket.summary.dto.SummaryAssembler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SummaryService {

    private final OpenAIClient ai;
    private final ObjectMapper om = new ObjectMapper();

    private final LoanProductRepository loanRepo;
    private final CreditLoanProductRepository creditRepo;
    private final CreditLoanOptionRepository optionRepo;
    private final SupportProductRepository supportRepo;

    private final PostRepository postRepo;

    /** 대출/신용 상품용 프롬프트 (LOAN 계열) */
    private static final String SYSTEM_LOAN = """
너는 한국의 금융/정부지원 상품을 소개하는 카피라이터이자 금융 상품 분석 전문가야.
주어진 원문을 바탕으로 아래 JSON만 출력해:

{
  "short": "<누구에게 어떤 특징의 무슨 상품인지 한 문장(28~60자).>",
  "long": "<아래 섹션 형식에 정확히 맞춘 상세 요약>"
}

[섹션 형식 - 반드시 이 순서/형식을 지켜]
주요내용
- 금리/한도/기간/상환방식 등 핵심 스펙 2~3줄
- 신청/이용에 영향을 주는 필수 조건 포함

대상
- 구체적 자격 요건(연령·소득·재직/학적·신용 등)을 간결히 요약

특징
- 타 상품 대비 차별점 1~2개 (수치/조건 포함)

신청방법
- 어디서/어떻게 신청하는지 큰 흐름만 요약

필요서류
- 신청 과정에서 필요한 서류를 문장 또는 쉼표 나열로 작성  
- 예: 신분증, 소득 증빙서류, 재직증명서 등

유의사항
- 상품 이용 시 꼭 알아야 할 조건/제한/불이익을 문장으로 정리  
- 예: 지원 제외 조건, 만기/중도상환 제한, 금리 변동 가능성 등

[작성 규칙]
- short는 카드형 요약, long은 상세페이지용.
- 친근하고 간결하되 과장/광고성 금지. 이모지 금지.**금지
- 출력은 반드시 JSON 하나만.
""";

    /** 정책지원 상품용 프롬프트 (SUPPORT) */
    private static final String SYSTEM_SUPPORT = """
너는 한국의 금융/정부지원 상품을 소개하는 카피라이터이자 금융 상품 분석 전문가야.
주어진 원문을 바탕으로 아래 JSON만 출력해:

{
  "short": "<누구에게 어떤 특징의 무슨 지원인지 한 문장(28~60자).>",
  "long": "<아래 섹션 형식에 정확히 맞춘 상세 요약>"
}

[섹션 형식 - 반드시 이 순서/형식을 지켜]
주요내용
- 사업/지원의 목적과 핵심 구조 2~3줄

지원대상
- 연령/소득/학력/재직/지역 등 세부 자격 요건

지원내용
- 금액/비율/횟수/기간 등 실제 제공 혜택을 수치 중심으로

신청방법
- 신청 창구/시기/절차 요약

필요서류
- 신청 과정에서 필요한 서류를 문장 또는 쉼표 나열로 작성  
- 예: 신분증, 소득 증빙서류, 사업자등록증 등

유의사항
- 지원 제외 대상, 중복수혜 제한, 사후 의무, 환수/취소 조건 등을 문장으로 정리

[작성 규칙]
- short는 카드형 요약, long은 상세페이지용.
- 친근하고 간결하되 과장/광고성 금지. 이모지 금지. ** 금지
- 출력은 반드시 JSON 하나만.
""";

    // 간단한 홀더
    private record Pair(String s, String l) {}

    /** LoanProduct → Post만 업데이트 */
//    @Transactional
//    public void generateForLoan(long id) {
//        LoanProduct p = loanRepo.findById(id).orElseThrow();     // 프롬프트용 조회
//        String user = SummaryAssembler.forLoan(p);
//        String json = ai.chatJson(SYSTEM_LOAN, user, 0.2);
//
//        Pair pair = parseJson(json); // ✅ 엔티티 세팅/저장 안 함
//        postRepo.updateSummaryBySource("LoanProduct", p.getId(), pair.s(), pair.l());
//    }
    @Transactional(readOnly = true)
    public String[] generateForLoan(long id) {
        try {
            // 1. LoanProduct 조회 (없을 경우 예외 발생)
            LoanProduct p = loanRepo.findById(id).orElseThrow(
                    () -> new RuntimeException("LoanProduct not found: " + id));

            // 2. AI 프롬프트 생성 및 JSON 결과 요청
            String user = SummaryAssembler.forLoan(p);
            String json = ai.chatJson(SYSTEM_LOAN, user, 0.2);

            // 3. JSON 파싱
            Pair pair = parseJson(json);

            // 4. 성공 로그 추가
            log.info("LoanProduct({}) AI 요약 생성 완료 - short: {}, long: {} chars",
                    id, pair.s.length(), pair.l.length());

            // 5. DB 직접 수정 대신, 결과물을 배열로 반환
            return new String[]{pair.l, pair.s}; // [0]: long, [1]: short

        } catch (Exception e) {
            // 6. 예외 발생 시 에러 로그 남기고, 기본값 반환
            log.error("LoanProduct({}) AI 요약 생성 실패: {}", id, e.getMessage());
            return new String[]{"상품 정보를 준비 중입니다.", "상품 정보 준비중"};
        }
    }
    /** CreditLoanProduct → Post만 업데이트 */
//    @Transactional
//    public void generateForCredit(long id) {
//        CreditLoanProduct p = creditRepo.findById(id).orElseThrow();
//        List<CreditLoanOption> opts = optionRepo.findByFinPrdtCd(p.getFinPrdtCd());
//        String user = SummaryAssembler.forCredit(p, opts);
//        String json = ai.chatJson(SYSTEM, user, 0.2);
//
//        Pair pair = parseJson(json);
//        postRepo.findBySourceTableAndSourceId("CreditLoanProduct", id);
//
//    }

    @Transactional(readOnly = true)
    public String[] generateForCredit(long id) {
        try {
            CreditLoanProduct p = creditRepo.findById(id).orElseThrow(
                    () -> new RuntimeException("CreditLoanProduct not found: " + id));

            List<CreditLoanOption> opts = optionRepo.findByFinPrdtCd(p.getFinPrdtCd());
            String user = SummaryAssembler.forCredit(p, opts);
            String json = ai.chatJson(SYSTEM_LOAN, user, 0.2);

            Pair pair = parseJson(json);
            log.info("CreditLoanProduct({}) AI 요약 생성 완료 - short: {}, long: {} chars",
                    id, pair.s.length(), pair.l.length());

            return new String[]{pair.l, pair.s}; // [0]: long, [1]: short

        } catch (Exception e) {
            log.error("CreditLoanProduct({}) AI 요약 생성 실패: {}", id, e.getMessage());
            return new String[]{"상품 정보를 준비 중입니다.", "상품 정보 준비중"}; // 기본값 반환
        }
    }

    /** SupportProduct → Post만 업데이트 */
//    @Transactional
//    public void generateForSupport(long id) {
//        SupportProduct s = supportRepo.findById(id).orElseThrow();
//        String user = SummaryAssembler.forSupport(s);
//        String json = ai.chatJson(SYSTEM, user, 0.2);
//
//        Pair pair = parseJson(json);
//        // ⚠️ PK 접근자 이름 확인: getId()/getServiceId() 중 프로젝트에 맞게 사용
//        postRepo.findBySourceTableAndSourceId("SupportProduct", id);
//
//    }

    @Transactional(readOnly = true)
    public String[] generateForSupport(long id) {
        try {
            SupportProduct s = supportRepo.findById(id).orElseThrow(
                    () -> new RuntimeException("SupportProduct not found: " + id));

            String user = SummaryAssembler.forSupport(s);
            String json = ai.chatJson(SYSTEM_SUPPORT, user, 0.2);

            Pair pair = parseJson(json);
            log.info("SupportProduct({}) AI 요약 생성 완료 - short: {}, long: {} chars",
                    id, pair.s.length(), pair.l.length());

            return new String[]{pair.l, pair.s}; // [0]: long, [1]: short

        } catch (Exception e) {
            log.error("SupportProduct({}) AI 요약 생성 실패: {}", id, e.getMessage());
            return new String[]{"지원 정보를 준비 중입니다.", "지원 정보 준비중"}; // 기본값 반환
        }
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
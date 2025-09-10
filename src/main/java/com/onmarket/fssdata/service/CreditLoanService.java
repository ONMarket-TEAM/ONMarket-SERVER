package com.onmarket.fssdata.service;

import com.onmarket.fssdata.domain.CreditLoanOption;
import com.onmarket.fssdata.domain.CreditLoanProduct;
import com.onmarket.fssdata.dto.CreditLoanApiResponse;
import com.onmarket.fssdata.repository.CreditLoanOptionRepository;
import com.onmarket.fssdata.repository.CreditLoanProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditLoanService {

    private final CreditLoanProductRepository productRepository;
    private final CreditLoanOptionRepository optionRepository;
    private final RestTemplate restTemplate;

    @Value("${fss.api.base-url}")
    private String baseUrl;

    @Value("${fss.api.auth-key}")
    private String authKey;

    // RestTemplate에 기본 헤더 설정
    private org.springframework.http.HttpEntity<?> createHttpEntity() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json; charset=utf-8");
        return new org.springframework.http.HttpEntity<>(headers);
    }

    // 모든 권역의 데이터를 가져와서 저장
    public void fetchAndSaveAllCreditLoanData() {
        // 신용대출을 제공하는 권역만 포함
        String[] topFinGrpNos = {"020000", "030200", "030300"}; // 은행, 여신전문금융회사, 저축은행만

        for (String topFinGrpNo : topFinGrpNos) {
            log.info("권역 코드 {} 데이터 수집 시작", topFinGrpNo);
            try {
                fetchAndSaveCreditLoanData(topFinGrpNo);
                log.info("권역 코드 {} 데이터 수집 완료", topFinGrpNo);
            } catch (Exception e) {
                log.error("권역 코드 {} 데이터 수집 실패: {}", topFinGrpNo, e.getMessage());
            }
        }
    }

    // API 데이터 수집 메서드
    public void fetchAndSaveCreditLoanData(String topFinGrpNo) {
        int pageNo = 1;
        int maxPageNo = 1;

        do {
            try {
                String url = UriComponentsBuilder.fromHttpUrl("https://finlife.fss.or.kr/finlifeapi/creditLoanProductsSearch.json")
                        .queryParam("auth", authKey)
                        .queryParam("topFinGrpNo", topFinGrpNo)
                        .queryParam("pageNo", pageNo)
                        .build()
                        .toUriString();

                log.info("완전한 API URL: {}", url);
                log.info("API 호출: 권역={}, 페이지={}", topFinGrpNo, pageNo);

                // 헤더와 함께 API 호출
                org.springframework.http.HttpEntity<?> httpEntity = createHttpEntity();

                // 1. 먼저 String으로 받아서 응답 확인
                org.springframework.http.ResponseEntity<String> responseEntity =
                        restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, httpEntity, String.class);

                String rawResponse = responseEntity.getBody();

                // HTTP 상태코드와 헤더 로깅
                log.info("HTTP 상태코드: {}", responseEntity.getStatusCode());
                log.info("응답 헤더: {}", responseEntity.getHeaders());
                log.info("응답 바디 존재 여부: {}", rawResponse != null);

                if (rawResponse != null) {
                    log.info("실제 응답 내용: {}", rawResponse);
                }

                if (rawResponse == null || rawResponse.trim().isEmpty()) {
                    log.error("API에서 빈 응답 받음");
                    break;
                }

                log.debug("Raw 응답 길이: {}", rawResponse.length());
                log.debug("Raw 응답 내용 (처음 500자): {}",
                        rawResponse.length() > 500 ? rawResponse.substring(0, 500) + "..." : rawResponse);

                // 2. JSON 파싱 시도
                CreditLoanApiResponse response;
                try {
                    response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, httpEntity, CreditLoanApiResponse.class).getBody();
                    if (response == null) {
                        log.error("JSON 파싱 결과가 null - 구조 불일치 가능성");
                        log.error("파싱 실패한 Raw 응답: {}", rawResponse);
                        break;
                    }
                } catch (Exception parseException) {
                    log.error("JSON 파싱 실패: {}", parseException.getMessage());
                    log.error("파싱 실패한 Raw 응답: {}", rawResponse);
                    break;
                }

                if (response.getResult() == null) {
                    log.error("result 필드가 null입니다");
                    log.error("응답 구조 확인 필요: {}", rawResponse);
                    break;
                }

                String errCd = response.getResult().getErrCd();
                String errMsg = response.getResult().getErrMsg();

                log.debug("API 응답 - 오류코드: {}, 메시지: {}", errCd, errMsg);

                if (!"000".equals(errCd)) {
                    log.error("API 오류: {} - {}", errCd, errMsg);
                    handleApiError(errCd, errMsg);
                    break;
                }

                maxPageNo = response.getResult().getMaxPageNo();
                int totalCount = response.getResult().getTotalCount();

                log.info("총 상품 수: {}, 현재 페이지: {}, 최대 페이지: {}", totalCount, pageNo, maxPageNo);

                // 데이터가 없는 경우 로그 출력 후 종료
                if (totalCount == 0) {
                    log.info("권역 {}에는 신용대출 상품이 없습니다.", topFinGrpNo);
                    break;
                }

                // 데이터 저장
                saveCreditLoanData(response);
                log.info("페이지 {}/{} 처리 완료", pageNo, maxPageNo);

                pageNo++;

                // API 호출 간격 조절 (Rate Limit 방지)
                if (pageNo <= maxPageNo) {
                    Thread.sleep(1000);
                }

            } catch (org.springframework.web.client.HttpClientErrorException e) {
                log.error("HTTP 클라이언트 오류 (4xx): {}", e.getMessage());
                log.error("HTTP 상태 코드: {}", e.getStatusCode());
                log.error("응답 본문: {}", e.getResponseBodyAsString());
                if (e.getStatusCode().value() == 429) {
                    log.warn("Rate Limit 도달, 5초 대기 후 재시도");
                    try {
                        Thread.sleep(5000);
                        continue; // 재시도
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                break;
            } catch (org.springframework.web.client.HttpServerErrorException e) {
                log.error("HTTP 서버 오류 (5xx): {}", e.getMessage());
                log.error("HTTP 상태 코드: {}", e.getStatusCode());
                break;
            } catch (org.springframework.web.client.ResourceAccessException e) {
                log.error("네트워크 연결 오류: {}", e.getMessage());
                break;
            } catch (Exception e) {
                log.error("예상치 못한 오류 발생: ", e);
                break;
            }
        } while (pageNo <= maxPageNo);
    }

    private String getBankSite(String korCoNm) {
        return switch (korCoNm) {
            case "우리은행" -> "https://www.wooribank.com";
            case "한국스탠다드차타드은행" -> "https://www.sc.co.kr";
            case "한국씨티은행" -> "https://www.citibank.co.kr";
            case "아이엠뱅크" -> "https://www.im.bank";
            case "부산은행" -> "https://www.busanbank.co.kr";
            case "광주은행" -> "https://www.kjbank.com";
            case "제주은행" -> "https://www.ejbank.co.kr";
            case "전북은행" -> "https://www.jbbank.co.kr";
            case "경남은행" -> "https://www.knbank.co.kr";
            case "중소기업은행" -> "https://www.ibk.co.kr";
            case "한국산업은행" -> "https://www.kdb.co.kr";
            case "국민은행" -> "https://www.kbstar.com";
            case "신한은행" -> "https://www.shinhan.com";
            case "농협은행주식회사" -> "https://www.nonghyup.com";
            case "주식회사 하나은행" -> "https://www.kebhana.com";
            case "주식회사 케이뱅크" -> "https://www.kbanknow.com";
            case "수협은행" -> "https://www.suhyup-bank.co.kr";
            case "주식회사 카카오뱅크" -> "https://www.kakaobank.com";
            case "토스뱅크 주식회사" -> "https://tossbank.com";
            case "애큐온저축은행" -> "https://www.aqbank.co.kr";
            case "OSB저축은행" -> "https://www.osb.co.kr";
            case "디비저축은행" -> "https://www.dbsave.co.kr";
            case "키움예스저축은행" -> "https://www.kiwoomyessb.com";
            case "SBI저축은행" -> "https://www.sbisb.co.kr";
            case "다올저축은행" -> "https://www.daolsb.com";
            case "고려저축은행" -> "https://www.koreasb.com";
            case "DH저축은행" -> "https://www.dhsb.co.kr";
            case "모아저축은행" -> "https://www.moasb.co.kr";
            case "키움저축은행" -> "https://www.kiwombank.co.kr";
            case "세람저축은행" -> "https://www.seramsb.co.kr";
            case "페퍼저축은행" -> "https://www.peppersb.com";
            case "한화저축은행" -> "https://www.hanwhasb.com";
            case "우리금융저축은행" -> "https://www.woorifinancesb.com";
            case "청주저축은행" -> "https://www.cjsb.co.kr";
            case "한성저축은행" -> "https://www.hansungsb.co.kr";
            case "상상인플러스저축은행" -> "https://www.sangsanginsb.com";
            case "스타저축은행" -> "https://www.starsb.co.kr";
            case "동양저축은행" -> "https://www.dongyangsb.com";
            case "스마트저축은행" -> "https://www.smartsb.co.kr";
            case "한국투자저축은행" -> "https://www.koreainvestsb.com";
            case "JT저축은행" -> "https://www.jtsb.co.kr";
            case "엔에이치저축은행" -> "https://www.nhsb.co.kr";
            case "IBK저축은행" -> "https://www.ibksb.co.kr";
            case "BNK저축은행" -> "https://www.bnksb.co.kr";
            case "KB저축은행" -> "https://www.kbsb.co.kr";
            case "하나저축은행" -> "https://www.hanasb.co.kr";
            case "JT친애저축은행" -> "https://www.jtchinaesb.com";
            case "신한저축은행" -> "https://www.shinhansb.com";
            case "웰컴저축은행" -> "https://www.welcomebank.co.kr";
            case "OK저축은행" -> "https://www.okbank.co.kr";

            default -> null;
        };
    }


    // 데이터베이스에 저장
    @Transactional
    public void saveCreditLoanData(CreditLoanApiResponse response) {
        log.debug("데이터 저장 시작");


        // 기본 상품 정보 저장
        if (response.getResult().getBaseList() != null && !response.getResult().getBaseList().isEmpty()) {
            log.info("상품 정보 저장 시작 - 총 {}개", response.getResult().getBaseList().size());

            List<CreditLoanProduct> productsToSave = new ArrayList<>();

            response.getResult().getBaseList().forEach(baseInfo -> {
                try {
                    // 중복 체크
                    CreditLoanProduct existing = productRepository.findByFinPrdtCd(baseInfo.getFinPrdtCd());

                    CreditLoanProduct product;
                    if (existing == null) {
                        String rltSite = getBankSite(baseInfo.getKorCoNm());

                        product = CreditLoanProduct.builder()
                                .dclsMonth(baseInfo.getDclsMonth())
                                .finCoNo(baseInfo.getFinCoNo())
                                .korCoNm(baseInfo.getKorCoNm())
                                .finPrdtCd(baseInfo.getFinPrdtCd())
                                .finPrdtNm(baseInfo.getFinPrdtNm())
                                .joinWay(baseInfo.getJoinWay())
                                .crdtPrdtType(baseInfo.getCrdtPrdtType())
                                .crdtPrdtTypeNm(baseInfo.getCrdtPrdtTypeNm())
                                .cbName(baseInfo.getCbName())
                                .dclsStrtDay(baseInfo.getDclsStrtDay())
                                .dclsEndDay(baseInfo.getDclsEndDay())
                                .finCoSubmDay(baseInfo.getFinCoSubmDay())
                                .rltSite(rltSite)
                                .build();
                        log.debug("새 상품 생성: {}", baseInfo.getFinPrdtCd());
                    } else {
                        // 기존 상품 업데이트 - 업데이트 메서드 사용
                        existing.updateProductInfo(
                                baseInfo.getDclsMonth(),
                                baseInfo.getKorCoNm(),
                                baseInfo.getFinPrdtNm(),
                                baseInfo.getJoinWay(),
                                baseInfo.getCrdtPrdtType(),
                                baseInfo.getCrdtPrdtTypeNm(),
                                baseInfo.getCbName(),
                                baseInfo.getDclsStrtDay(),
                                baseInfo.getDclsEndDay(),
                                baseInfo.getFinCoSubmDay(),
                                getBankSite(baseInfo.getKorCoNm())


                        );
                        product = existing;
                        log.debug("기존 상품 업데이트: {}", baseInfo.getFinPrdtCd());
                    }

                    productsToSave.add(product);

                } catch (Exception e) {
                    log.error("상품 매핑 실패: {} - {}", baseInfo.getFinPrdtCd(), e.getMessage());
                }
            });

            // 배치 저장
            if (!productsToSave.isEmpty()) {
                productRepository.saveAll(productsToSave);
                log.info("상품 정보 저장 완료 - {}개", productsToSave.size());

                // 저장 후 실제 DB 카운트 확인
                long totalAfterSave = productRepository.count();
                log.info("저장 후 DB 총 상품 수: {}", totalAfterSave);
            }

        } else {
            log.warn("baseList가 비어있음 - 저장할 상품 정보가 없습니다");
        }

        // 옵션 정보 저장
        if (response.getResult().getOptionList() != null && !response.getResult().getOptionList().isEmpty()) {
            log.info("옵션 정보 저장 시작 - 총 {}개", response.getResult().getOptionList().size());

            List<CreditLoanOption> optionsToSave = new ArrayList<>();

            response.getResult().getOptionList().forEach(optionInfo -> {
                try {
                    // 중복 체크
                    boolean exists = optionRepository.existsByFinCoNoAndFinPrdtCdAndCrdtLendRateType(
                            optionInfo.getFinCoNo(),
                            optionInfo.getFinPrdtCd(),
                            optionInfo.getCrdtLendRateType()
                    );

                    if (!exists) {
                        // @Builder 패턴 사용하여 옵션 생성
                        CreditLoanOption option = CreditLoanOption.builder()
                                .finCoNo(optionInfo.getFinCoNo())
                                .finPrdtCd(optionInfo.getFinPrdtCd())
                                .crdtLendRateType(optionInfo.getCrdtLendRateType())
                                .crdtLendRateTypeNm(optionInfo.getCrdtLendRateTypeNm())
                                .crdtGrad1(optionInfo.getCrdtGrad1())
                                .crdtGrad4(optionInfo.getCrdtGrad4())
                                .crdtGrad5(optionInfo.getCrdtGrad5())
                                .crdtGrad6(optionInfo.getCrdtGrad6())
                                .crdtGrad10(optionInfo.getCrdtGrad10())
                                .crdtGrad11(optionInfo.getCrdtGrad11())
                                .crdtGrad12(optionInfo.getCrdtGrad12())
                                .crdtGrad13(optionInfo.getCrdtGrad13())
                                .crdtGradAvg(optionInfo.getCrdtGradAvg())
                                .creditLoanProduct(null) // 관계 설정이 필요한 경우 추가
                                .build();

                        optionsToSave.add(option);
                        log.debug("새 옵션 추가: {}", optionInfo.getFinPrdtCd());
                    } else {
                        log.debug("중복 옵션 스킵: {}", optionInfo.getFinPrdtCd());
                    }

                } catch (Exception e) {
                    log.error("옵션 매핑 실패: {} - {}", optionInfo.getFinPrdtCd(), e.getMessage());
                }
            });

            // 배치 저장
            if (!optionsToSave.isEmpty()) {
                optionRepository.saveAll(optionsToSave);
                log.info("옵션 정보 저장 완료 - {}개", optionsToSave.size());
            }

        } else {
            log.warn("optionList가 비어있음 - 저장할 옵션 정보가 없습니다");
        }

        log.info("데이터 저장 완료");
    }

    // API 오류 처리
    public void handleApiError(String errCd, String errMsg) {
        switch (errCd) {
            case "010":
                log.error("인증키 오류 - 인증키를 확인하세요: {}", errMsg);
                break;
            case "020":
                log.error("필수 파라미터 누락: {}", errMsg);
                break;
            case "021":
                log.error("조회 기간 오류: {}", errMsg);
                break;
            case "022":
                log.error("조회 건수 초과: {}", errMsg);
                break;
            case "100":
                log.error("시스템 오류: {}", errMsg);
                break;
            default:
                log.error("알 수 없는 오류: {} - {}", errCd, errMsg);
        }
    }

    // 전체 상품 수 조회
    public long getTotalProductCount() {
        return productRepository.count();
    }

    // 전체 옵션 수 조회
    public long getTotalOptionCount() {
        return optionRepository.count();
    }

    // 모든 상품 조회 (페이징)
    public List<CreditLoanProduct> getAllProducts(int page, int size) {
        return productRepository.findAll(
                org.springframework.data.domain.PageRequest.of(page, size)
        ).getContent();
    }

    // 특정 금융회사의 상품 조회
    public List<CreditLoanProduct> getProductsByCompany(String companyName) {
        return productRepository.findByKorCoNmContaining(companyName);
    }

    // 특정 상품의 금리 옵션 조회
    public List<CreditLoanOption> getOptionsByProduct(String finPrdtCd) {
        return optionRepository.findByFinPrdtCd(finPrdtCd);
    }

    // API 테스트용 메서드
    public String testApiCall() {
        String url = UriComponentsBuilder.fromHttpUrl("https://finlife.fss.or.kr/finlifeapi/creditLoanProductsSearch.json")
                .queryParam("auth", authKey)
                .queryParam("topFinGrpNo", "020000")
                .queryParam("pageNo", 1)
                .build()
                .toUriString();

        log.info("테스트 URL: {}", url);
        try {
            // 헤더와 함께 API 호출
            org.springframework.http.HttpEntity<?> httpEntity = createHttpEntity();
            return restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, httpEntity, String.class).getBody();
        } catch (Exception e) {
            log.error("API 테스트 호출 실패: {}", e.getMessage());
            throw e;
        }
    }
}
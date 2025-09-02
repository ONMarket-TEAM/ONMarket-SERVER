package com.onmarket.loandata.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.onmarket.loandata.domain.LoanProduct;
import com.onmarket.loandata.dto.XmlLoanApiResponse;
import com.onmarket.loandata.repository.LoanProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanProductService {

    private final LoanProductRepository loanProductRepository;
    private final RestTemplate restTemplate;
    private final XmlMapper xmlMapper = new XmlMapper();

    @Value("${gov.api.loan.service-key}")
    private String serviceKey;

    @Value("${gov.api.loan.base-url}")
    private String baseUrl;

    // 모든 대출 상품 데이터 수집
    @Transactional
    public void fetchAndSaveAllLoanProducts() {
        int pageNo = 1;
        int numOfRows = 100;
        int totalProcessed = 0;

        do {
            try {
                log.info("대출 상품 데이터 수집 시작 - 페이지: {}", pageNo);
                XmlLoanApiResponse response = fetchLoanProducts(pageNo, numOfRows);
                if (response == null || response.getHeader() == null || !"00".equals(response.getHeader().getResultCode())) {
                    log.error("API 오류 또는 응답 없음: {}", response != null && response.getHeader() != null ? response.getHeader().getResultMsg() : "응답 없음");
                    break;
                }
                if (response.getBody() == null || response.getBody().getItems() == null || response.getBody().getItems().getItem() == null) {
                    log.info("수집할 데이터가 더 이상 없습니다.");
                    break;
                }

                List<XmlLoanApiResponse.XmlLoanItem> items = response.getBody().getItems().getItem();
                log.info("페이지 {}에서 {}개 데이터 조회", pageNo, items.size());
                saveXmlLoanProducts(items);

                totalProcessed += items.size();
                log.info("페이지 {} 처리 완료, 누적 처리: {}개", pageNo, totalProcessed);

                if (items.size() < numOfRows) {
                    log.info("모든 데이터 수집 완료 - 총 {}개", totalProcessed);
                    break;
                }
                pageNo++;
                Thread.sleep(1000);

            } catch (Exception e) {
                log.error("데이터 수집 중 오류 발생: ", e);
                break;
            }
        } while (true);
    }

    // XML 대출 상품 API 호출
    private XmlLoanApiResponse fetchLoanProducts(int pageNo, int numOfRows) throws Exception {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/LoanProductSearchingInfo/getLoanProductSearchingInfo")
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows)
                .build()
                .toUriString();

        log.debug("API 호출 URL: {}", url);
        String xmlResponse = restTemplate.getForObject(url, String.class);

        if (xmlResponse == null || xmlResponse.trim().isEmpty()) {
            log.error("API로부터 빈 응답을 받았습니다.");
            return null;
        }
        return xmlMapper.readValue(xmlResponse, XmlLoanApiResponse.class);
    }

    // XML 대출 상품 데이터 저장
    @Transactional
    public void saveXmlLoanProducts(List<XmlLoanApiResponse.XmlLoanItem> items) {
        List<LoanProduct> productsToSave = new ArrayList<>();
        items.forEach(item -> {
            try {
                Optional<LoanProduct> existingProduct = loanProductRepository.findBySequence(item.getSeq());

                LoanProduct product;
                if (existingProduct.isPresent()) {
                    // 기존 상품 업데이트
                    product = existingProduct.get();
                    updateExistingProduct(item, product);
                    log.debug("기존 상품 업데이트: {}", item.getSeq());
                } else {
                    // 새 상품 생성 - @Builder 사용
                    product = createNewProduct(item);
                    log.debug("새 상품 생성: {}", item.getSeq());
                }

                productsToSave.add(product);
            } catch (Exception e) {
                log.error("상품 매핑 실패: {} - {}", item.getSeq(), e.getMessage());
            }
        });

        if (!productsToSave.isEmpty()) {
            loanProductRepository.saveAll(productsToSave);
            log.info("대출 상품 정보 저장 완료 - {}개", productsToSave.size());
        }
    }

    // 새 상품 생성 - @Builder 패턴 사용
    private LoanProduct createNewProduct(XmlLoanApiResponse.XmlLoanItem item) {
        // 기관명에 따른 신청 URL 자동 설정
        String applicationUrl = generateApplicationUrl(item.getOfrInstNm());

        return LoanProduct.builder()
                .sequence(item.getSeq())
                .productName(truncateString(item.getFinPrdNm(), 255))
                .loanLimit(item.getLnLmt())
                .limitOver1000(item.getLnLmt1000Abnml())
                .limitOver2000(item.getLnLmt2000Abnml())
                .limitOver3000(item.getLnLmt3000Abnml())
                .limitOver5000(item.getLnLmt5000Abnml())
                .limitOver10000(item.getLnLmt10000Abnml())
                .interestCategory(item.getIrtCtg())
                .interestRate(item.getIrt())
                .maxTotalTerm(item.getMaxTotLnTrm())
                .maxDeferredTerm(item.getMaxDfrmTrm())
                .maxRepaymentTerm(item.getMaxRdptTrm())
                .repaymentMethod(item.getRdptMthd())
                .usage(item.getUsge())
                .target(item.getTrgt())
                .institutionCategory(item.getInstCtg())
                .offeringInstitution(item.getOfrInstNm())
                .specialTargetConditions(item.getSuprTgtDtlCond())
                .age(item.getAge())
                .ageBelow39(item.getAge39Blw())
                .income(item.getIncm())
                .handlingInstitution(item.getHdlInst())
                .relatedSite(applicationUrl)
                .build();
    }

    // 기존 상품 업데이트 - 비즈니스 메서드 사용
    private void updateExistingProduct(XmlLoanApiResponse.XmlLoanItem item, LoanProduct product) {
        // 기존 상품의 URL이 없는 경우에만 새로 설정 (수동 설정된 URL 보호)
        if (product.getRelatedSite() == null || product.getRelatedSite().isEmpty()) {
            String applicationUrl = generateApplicationUrl(item.getOfrInstNm());
            product.setRelatedSite(applicationUrl);
        }

        product.updateFromXmlData(
                item.getSeq(),
                truncateString(item.getFinPrdNm(), 255),
                item.getLnLmt(),
                item.getLnLmt1000Abnml(),
                item.getLnLmt2000Abnml(),
                item.getLnLmt3000Abnml(),
                item.getLnLmt5000Abnml(),
                item.getLnLmt10000Abnml(),
                item.getIrtCtg(),
                item.getIrt(),
                item.getMaxTotLnTrm(),
                item.getMaxDfrmTrm(),
                item.getMaxRdptTrm(),
                item.getRdptMthd(),
                item.getUsge(),
                item.getTrgt(),
                item.getInstCtg(),
                item.getOfrInstNm(),
                item.getSuprTgtDtlCond(),
                item.getAge(),
                item.getAge39Blw(),
                item.getIncm(),
                item.getHdlInst()
        );
    }

    private String truncateString(String str, int maxLength) {
        if (str == null) return null;
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    /**
     * 기관명에 따른 신청 URL 생성
     * 각 기관별로 적절한 신청 페이지 URL 매핑
     */
    private String generateApplicationUrl(String institutionName) {
        if (institutionName == null) {
            return null;
        }

        // 서민금융진흥원
        if (institutionName.contains("서민금융진흥원") ||
                institutionName.contains("미소금융") ||
                institutionName.contains("서민금융")) {
            return "https://www.kinfa.or.kr/main.do"; // 서민금융 잇다 앱
        }

        // 신용보증재단들
        if (institutionName.contains("신용보증재단")) {
            return "https://www.koreg.or.kr/"; // 신용보증재단중앙회
        }

        // 개별 지역 신용보증재단
        if (institutionName.contains("서울신용보증재단")) {
            return "https://www.seoulshinbo.co.kr/";
        } else if (institutionName.contains("울산신용보증재단")) {
            return "https://www.ulsanshinbo.co.kr/main/";
        } else if (institutionName.contains("전남신용보증재단")) {
            return "https://www.jnsinbo.or.kr/jnsinbo/intro.do";
        } else if (institutionName.contains("대전신용보증재단")) {
            return "https://www.sinbo.or.kr/";
        } else if (institutionName.contains("광주신용보증재단")) {
            return "https://www.gjsinbo.or.kr/";
        } else if (institutionName.contains("부산신용보증재단")) {
            return "https://www.busansinbo.or.kr/main.do";
        } else if (institutionName.contains("전북신용보증재단")) {
            return "https://www.jbcredit.or.kr/";
        } else if (institutionName.contains("충북신용보증재단")) {
            return "https://www.cbsig.or.kr/";
        } else if (institutionName.contains("충남신용보증재단")) {
            return "https://www.cbsinbo.or.kr/";
        } else if (institutionName.contains("강원신용보증재단")) {
            return "https://www.gwsinbo.or.kr/main/intro.php";
        } else if (institutionName.contains("경기신용보증재단")) {
            return "https://www.gcgf.or.kr/gcgf/intro.do";
        } else if (institutionName.contains("경남신용보증재단")) {
            return "https://www.gnsinbo.or.kr/";
        } else if (institutionName.contains("경북신용보증재단")) {
            return "https://gbsinbo.co.kr/";
        } else if (institutionName.contains("대구신용보증재단")) {
            return "https://www.ttg.co.kr/";
        } else if (institutionName.contains("세종신용보증재단")) {
            return "https://www.sjsinbo.or.kr/";
        } else if (institutionName.contains("인천신용보증재단")) {
            return "https://www.icsinbo.or.kr/";
        } else if (institutionName.contains("제주신용보증재단")) {
            return "https://www.jcgf.or.kr/index2.php";
        }

        // 정부기관/공단
        if (institutionName.contains("근로복지공단")) {
            return "https://www.comwel.or.kr/";
        } else if (institutionName.contains("소상공인시장진흥공단")) {
            return "https://www.semas.or.kr/";
        } else if (institutionName.contains("충청북도기업진흥원")) {
            return "https://www.cbtp.or.kr/";
        } else if (institutionName.contains("경남투자경제진흥원")) {
            return "https://giba.or.kr/intro/NR_index.do";
        }

        // 주택금융 관련
        if (institutionName.contains("주택도시보증공사")) {
            return "https://www.khug.or.kr/";
        } else if (institutionName.contains("한국주택금융공사")) {
            return "https://www.khug.or.kr/index_hug_in.jsp";
        } else if (institutionName.contains("주택도시기금")) {
            return "https://nhuf.molit.go.kr/";
        }

        // 신협
        if (institutionName.contains("신협")) {
            return "https://www.cu.co.kr/";
        }

        // 은행
        if (institutionName.contains("BNK경남은행")) {
            return "https://www.bnksum.co.kr/";
        } else if (institutionName.contains("IBK기업은행")) {
            return "https://www.knbank.co.kr/ib20/mnu/BHP000000000001";
        } else if (institutionName.contains("국민은행")) {
            return "https://www.kbstar.com";
        } else if (institutionName.contains("신한은행")) {
            return "https://www.shinhan.com";
        } else if (institutionName.contains("우리은행")) {
            return "https://www.wooribank.com";
        } else if (institutionName.contains("하나은행")) {
            return "https://www.kebhana.com/";
        }

        // 기타 보증기관
        if (institutionName.contains("SGI서울보증")) {
            return "https://www.sgic.co.kr/";
        }

        // 기본 URL (정부24 대출상품 페이지)
        return "https://www.gov.kr/portal/onestopSvc/lonGoods";
    }

    // --- 검색 및 조회 메서드들 ---

    public long getTotalProductCount() {
        return loanProductRepository.count();
    }

    public List<LoanProduct> searchByProductName(String productName) {
        return loanProductRepository.findByProductNameContaining(productName);
    }



    public List<LoanProduct> getAllProducts(int page, int size) {
        return loanProductRepository.findAll(org.springframework.data.domain.PageRequest.of(page, size)).getContent();
    }

    // 데이터 수집 상태 확인 메서드
    @Transactional(readOnly = true)
    public Map<String, Object> getDataStatus() {
        Map<String, Object> status = new HashMap<>();
        long totalProducts = loanProductRepository.count();

        status.put("totalProducts", totalProducts);
        status.put("isEmpty", totalProducts == 0);
        status.put("timestamp", System.currentTimeMillis());

        return status;
    }

    // seq로 특정 상품 상세 조회 메서드
    @Transactional(readOnly = true)
    public LoanProduct getProductBySeq(String seq) {
        return loanProductRepository.findBySequence(seq)
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다. SEQ: " + seq));
    }
}
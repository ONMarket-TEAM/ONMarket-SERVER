package com.onmarket.supportsdata.service;

import com.onmarket.supportsdata.domain.SupportCondition;
import com.onmarket.supportsdata.domain.SupportProduct;
import com.onmarket.supportsdata.dto.ApiResponseDTO;
import com.onmarket.supportsdata.dto.ServiceDetailDTO;
import com.onmarket.supportsdata.dto.ServiceInfoDTO;
import com.onmarket.supportsdata.dto.SupportConditionDTO;
import com.onmarket.supportsdata.repository.SupportServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicDataServiceImpl implements PublicDataService {

    private final WebClient.Builder webClientBuilder;
    private final SupportServiceRepository supportServiceRepository;
    private final SimpleDateParser simpleDateParser;

    @Value("${gov.api.support.base-url}")
    private String baseUrl;

    @Value("${gov.api.support.service-key}")
    private String serviceKey;

    @Override
    @Transactional
    public Mono<Void> findAndSaveAllServices() {
        final int perPage = 100;
        WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();

        final String userCategory = "소상공인";

        return getAllServiceInfoDTOs(webClient, perPage, userCategory)
                .delayElements(Duration.ofMillis(100))
                .flatMap(serviceInfo -> {
                    String serviceId = serviceInfo.getServiceId();
                    Mono<ApiResponseDTO<ServiceDetailDTO>> detailMono = callDetailApi(webClient, serviceId);
                    Mono<ApiResponseDTO<SupportConditionDTO>> conditionMono = callConditionApi(webClient, serviceId);

                    return Mono.zip(Mono.just(serviceInfo), detailMono, conditionMono)
                            .doOnNext(tuple -> {
                                ServiceInfoDTO infoDTO = tuple.getT1();
                                tuple.getT2().getData().stream().findFirst().ifPresent(detailDTO -> {
                                    tuple.getT3().getData().stream().findFirst().ifPresent(conditionDTO -> {
                                        createAndSaveEntities(infoDTO, detailDTO, conditionDTO);
                                    });
                                });
                            })
                            .onErrorResume(e -> {
                                log.error("Failed to process serviceId {}: {}", serviceId, e.getMessage());
                                return Mono.empty();
                            });
                }, 5)
                .then();
    }

    private Flux<ServiceInfoDTO> getAllServiceInfoDTOs(WebClient webClient, int perPage, String userCategory) {
        return callApi(webClient, 1, perPage, userCategory, new ParameterizedTypeReference<ApiResponseDTO<ServiceInfoDTO>>() {})
                .expand(response -> {
                    int totalCount = response.getTotalCount();
                    int currentPage = response.getPage();
                    if (totalCount == 0 || currentPage * perPage >= totalCount) {
                        return Mono.empty();
                    }
                    int totalPages = (int) Math.ceil((double) totalCount / perPage);
                    if (currentPage < totalPages) {
                        return callApi(webClient, currentPage + 1, perPage, userCategory, new ParameterizedTypeReference<ApiResponseDTO<ServiceInfoDTO>>() {});
                    } else {
                        return Mono.empty();
                    }
                })
                .flatMapIterable(response -> Optional.ofNullable(response.getData()).orElse(Collections.emptyList()));
    }

    private <T> Mono<ApiResponseDTO<T>> callApi(WebClient webClient, int page, int perPage, String userCategory, ParameterizedTypeReference<ApiResponseDTO<T>> typeReference) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/gov24/v3/serviceList")
                        .queryParam("page", page)
                        .queryParam("perPage", perPage)
                        .queryParam("cond[사용자구분::LIKE]", userCategory)
                        .queryParam("serviceKey", serviceKey)
                        .build())
                .retrieve()
                .bodyToMono(typeReference)
                .doOnError(WebClientResponseException.class, err -> log.error("API Error - Status: [{}], Page: [{}], Body: {}", err.getStatusCode(), page, err.getResponseBodyAsString()));
    }

    private Mono<ApiResponseDTO<ServiceDetailDTO>> callDetailApi(WebClient webClient, String serviceId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/gov24/v3/serviceDetail")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("cond[서비스ID::EQ]", serviceId)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponseDTO<ServiceDetailDTO>>() {});
    }

    private Mono<ApiResponseDTO<SupportConditionDTO>> callConditionApi(WebClient webClient, String serviceId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/gov24/v3/supportConditions")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("cond[서비스ID::EQ]", serviceId)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponseDTO<SupportConditionDTO>>() {});
    }

    private void createAndSaveEntities(ServiceInfoDTO infoDTO, ServiceDetailDTO detailDTO, SupportConditionDTO conditionDTO) {
        String serviceId = infoDTO.getServiceId();

        if (supportServiceRepository.existsByServiceId(serviceId)) {
            log.info("Service ID {} already exists in the database. Skipping.", serviceId);
            return;
        }

        String generatedKeywords = generateKeywords(infoDTO, detailDTO, conditionDTO);

        SupportProduct serviceEntity = SupportProduct.builder()
                .serviceId(serviceId)
                .supportType(infoDTO.getSupportType())
                .serviceName(infoDTO.getServiceName())
                .servicePurposeSummary(infoDTO.getServicePurposeSummary())
                .supportTarget(infoDTO.getSupportTarget())
                .selectionCriteria(infoDTO.getSelectionCriteria())
                .supportContent(infoDTO.getSupportContent())
                .applicationMethod(infoDTO.getApplicationMethod())
                .detailUrl(infoDTO.getDetailUrl())
                .departmentName(infoDTO.getDepartmentName())
                .userCategory(infoDTO.getUserCategory())
                .servicePurpose(detailDTO.getServicePurpose())
                .applicationDeadline(detailDTO.getApplicationDeadline()) // 원본 보존
                .requiredDocuments(detailDTO.getRequiredDocuments())
                .receptionAgencyName(detailDTO.getReceptionAgencyName())
                .contact(detailDTO.getContact())
                .onlineApplicationUrl(detailDTO.getOnlineApplicationUrl())
                .laws(detailDTO.getLaws())
                .keywords(generatedKeywords)
                .build();

        // 신청 기한 파싱 및 설정
        SimpleDateParser.DatePair datePair = simpleDateParser.parseApplicationDeadline(
                detailDTO.getApplicationDeadline()
        );
        serviceEntity.setStartDay(datePair.startDay);
        serviceEntity.setEndDay(datePair.endDay);

        // SupportCondition 설정
        SupportCondition conditionEntity = SupportCondition.builder()
                .genderMale(conditionDTO.getGenderMale())
                .genderFemale(conditionDTO.getGenderFemale())
                .ageStart(conditionDTO.getAgeStart())
                .ageEnd(conditionDTO.getAgeEnd())
                .incomeBracket1(conditionDTO.getIncomeBracket1())
                .incomeBracket2(conditionDTO.getIncomeBracket2())
                .incomeBracket3(conditionDTO.getIncomeBracket3())
                .incomeBracket4(conditionDTO.getIncomeBracket4())
                .incomeBracket5(conditionDTO.getIncomeBracket5())
                .jobEmployee(conditionDTO.getJobEmployee())
                .jobSeeker(conditionDTO.getJobSeeker())
                .householdSinglePerson(conditionDTO.getHouseholdSinglePerson())
                .householdMultiChild(conditionDTO.getHouseholdMultiChild())
                .householdNoHome(conditionDTO.getHouseholdNoHome())
                .businessProspective(conditionDTO.getBusinessProspective())
                .businessOperating(conditionDTO.getBusinessOperating())
                .businessStruggling(conditionDTO.getBusinessStruggling())
                .build();

        conditionEntity.setSupportProduct(serviceEntity);
        supportServiceRepository.save(serviceEntity);

        log.debug("Saved service: {} with dates: {} ~ {}",
                serviceEntity.getServiceName(), datePair.startDay, datePair.endDay);
    }

    // 키워드 추출 - 5가지 업종으로 분류
    private String generateKeywords(ServiceInfoDTO info, ServiceDetailDTO detail, SupportConditionDTO condition) {
        Set<String> keywords = new HashSet<>();
        StringBuilder textBuilder = new StringBuilder();

        // 모든 관련 텍스트 수집
        if (info.getSupportTarget() != null) textBuilder.append(info.getSupportTarget()).append(" ");
        if (info.getServiceName() != null) textBuilder.append(info.getServiceName()).append(" ");
        if (info.getServicePurposeSummary() != null) textBuilder.append(info.getServicePurposeSummary()).append(" ");
        if (info.getSupportContent() != null) textBuilder.append(info.getSupportContent()).append(" ");
        if (detail.getServicePurpose() != null) textBuilder.append(detail.getServicePurpose()).append(" ");
        if (info.getUserCategory() != null) textBuilder.append(info.getUserCategory()).append(" ");

        String combinedText = textBuilder.toString().toLowerCase();

        // 1. 음식업 분류
        if (combinedText.contains("음식") || combinedText.contains("식당") || combinedText.contains("음식점") ||
                combinedText.contains("요리") || combinedText.contains("카페") || combinedText.contains("제과") ||
                combinedText.contains("베이커리") || combinedText.contains("치킨") || combinedText.contains("피자") ||
                combinedText.contains("한식") || combinedText.contains("중식") || combinedText.contains("일식") ||
                combinedText.contains("양식") || combinedText.contains("분식") || combinedText.contains("술집") ||
                combinedText.contains("주점") || combinedText.contains("호프") || combinedText.contains("바(bar)") ||
                combinedText.contains("식품제조") || combinedText.contains("도시락") || combinedText.contains("급식") ||
                combinedText.contains("식료품") || combinedText.contains("떡") || combinedText.contains("빵") ||
                combinedText.contains("휴게음식점") || combinedText.contains("일반음식점")) {
            keywords.add("음식업");
        }

        // 2. 소매업 분류
        if (combinedText.contains("소매") || combinedText.contains("판매") || combinedText.contains("상점") ||
                combinedText.contains("마트") || combinedText.contains("슈퍼") || combinedText.contains("편의점") ||
                combinedText.contains("전통시장") || combinedText.contains("상점가") || combinedText.contains("시장") ||
                combinedText.contains("도매") || combinedText.contains("유통") || combinedText.contains("무역") ||
                combinedText.contains("수출") || combinedText.contains("수입") || combinedText.contains("온라인쇼핑") ||
                combinedText.contains("전자상거래") || combinedText.contains("쇼핑몰") || combinedText.contains("백화점") ||
                combinedText.contains("의류") || combinedText.contains("패션") || combinedText.contains("화장품") ||
                combinedText.contains("잡화") || combinedText.contains("가전") || combinedText.contains("가구") ||
                combinedText.contains("서점") || combinedText.contains("약국") || combinedText.contains("꽃집")) {
            keywords.add("소매업");
        }

        // 3. 서비스업 분류
        if (combinedText.contains("서비스") || combinedText.contains("상담") || combinedText.contains("컨설팅") ||
                combinedText.contains("교육") || combinedText.contains("학원") || combinedText.contains("미용") ||
                combinedText.contains("이용") || combinedText.contains("헤어") || combinedText.contains("네일") ||
                combinedText.contains("피부") || combinedText.contains("마사지") || combinedText.contains("세탁") ||
                combinedText.contains("청소") || combinedText.contains("수리") || combinedText.contains("정비") ||
                combinedText.contains("호텔") || combinedText.contains("숙박") || combinedText.contains("펜션") ||
                combinedText.contains("여행") || combinedText.contains("관광") || combinedText.contains("운송") ||
                combinedText.contains("택시") || combinedText.contains("배달") || combinedText.contains("물류") ||
                combinedText.contains("부동산") || combinedText.contains("중개") || combinedText.contains("보험") ||
                combinedText.contains("금융") || combinedText.contains("은행") || combinedText.contains("의료") ||
                combinedText.contains("병원") || combinedText.contains("약국") || combinedText.contains("사무") ||
                combinedText.contains("디자인") || combinedText.contains("광고") || combinedText.contains("마케팅")) {
            keywords.add("서비스업");
        }

        // 4. 제조업 분류
        if (combinedText.contains("제조") || combinedText.contains("생산") || combinedText.contains("공장") ||
                combinedText.contains("소공인") || combinedText.contains("가공") || combinedText.contains("조립") ||
                combinedText.contains("봉제") || combinedText.contains("의류제조") || combinedText.contains("섬유") ||
                combinedText.contains("기계") || combinedText.contains("금속") || combinedText.contains("철강") ||
                combinedText.contains("인쇄") || combinedText.contains("출판") || combinedText.contains("주얼리") ||
                combinedText.contains("수제화") || combinedText.contains("가죽") || combinedText.contains("목재") ||
                combinedText.contains("가구제조") || combinedText.contains("플라스틱") || combinedText.contains("화학") ||
                combinedText.contains("전자") || combinedText.contains("반도체") || combinedText.contains("자동차") ||
                combinedText.contains("건자재") || combinedText.contains("건설자재")) {
            keywords.add("제조업");
        }

        // 5. 기타 분류
        if (combinedText.contains("농업") || combinedText.contains("어업") || combinedText.contains("축산") ||
                combinedText.contains("농축수산") || combinedText.contains("농림") || combinedText.contains("임업") ||
                combinedText.contains("건설") || combinedText.contains("토목") || combinedText.contains("인테리어") ||
                combinedText.contains("리모델링") || combinedText.contains("광업") || combinedText.contains("채굴") ||
                combinedText.contains("에너지") || combinedText.contains("전력") || combinedText.contains("가스") ||
                combinedText.contains("IT") || combinedText.contains("소프트웨어") || combinedText.contains("앱") ||
                combinedText.contains("프로그램") || combinedText.contains("웹") || combinedText.contains("콘텐츠") ||
                combinedText.contains("문화") || combinedText.contains("예술") || combinedText.contains("창작") ||
                combinedText.contains("스포츠") || combinedText.contains("오락") || combinedText.contains("게임")) {
            keywords.add("기타");
        }

        // 키워드가 없는 경우 기본값으로 "기타" 추가
        if (keywords.isEmpty()) {
            keywords.add("기타");
        }

        // 추가적으로 대상별 키워드도 포함 (기존 로직 유지)
        if (combinedText.contains("소상공인") || combinedText.contains("자영업자") || combinedText.contains("소기업")) {
            keywords.add("소상공인");
        }
        if (condition.getBusinessProspective() != null || combinedText.contains("예비창업")) {
            keywords.add("예비창업자");
        }
        if (combinedText.contains("청년")) {
            keywords.add("청년");
        }
        if (combinedText.contains("여성") || combinedText.contains("장애인") || combinedText.contains("다문화")) {
            keywords.add("취약계층");
        }

        // 지역 정보 추출
        String locationText = info.getDepartmentName();
        if (locationText != null) {
            Pattern pattern = Pattern.compile("(\\S+[시도])?\\s*(\\S+[군구])");
            Matcher matcher = pattern.matcher(locationText);
            if (matcher.find()) {
                if (matcher.group(1) != null) {
                    keywords.add(matcher.group(1).trim());
                }
                if (matcher.group(2) != null) {
                    keywords.add(matcher.group(2).trim());
                }
            }
        }

        return String.join(",", keywords);
    }
}
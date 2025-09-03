package com.onmarket.supportsdata.service;

import com.onmarket.supportsdata.domain.SupportCondition;
import com.onmarket.supportsdata.domain.SupportProduct;
import com.onmarket.supportsdata.dto.ApiResponseDTO;
import com.onmarket.supportsdata.dto.ServiceDetailDTO;
import com.onmarket.supportsdata.dto.ServiceInfoDTO;
import com.onmarket.supportsdata.dto.SupportConditionDTO;
import com.onmarket.supportsdata.repository.SupportProductRepository;
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
    private final SupportProductRepository supportProductRepository;

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

        // DB 저장 전 ID 중복 확인
        if (supportProductRepository.existsById(serviceId)) {
            log.info("Service ID {} already exists in the database. Skipping.", serviceId);
            return; // 이미 존재하면 저장을 건너뛰고 메소드를 종료합니다.
        }

        // 존재하지 않을 시 실행
        String generatedKeywords = generateKeywords(infoDTO, detailDTO, conditionDTO);

        SupportProduct serviceEntity = SupportProduct.builder()
                .serviceId(serviceId) // serviceId 변수 사용
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
                .applicationDeadline(detailDTO.getApplicationDeadline())
                .requiredDocuments(detailDTO.getRequiredDocuments())
                .receptionAgencyName(detailDTO.getReceptionAgencyName())
                .contact(detailDTO.getContact())
                .onlineApplicationUrl(detailDTO.getOnlineApplicationUrl())
                .laws(detailDTO.getLaws())
                .keywords(generatedKeywords)
                .build();

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
        supportProductRepository.save(serviceEntity);
    }

    // 키워드 추출
    private String generateKeywords(ServiceInfoDTO info, ServiceDetailDTO detail, SupportConditionDTO condition) {
        Set<String> keywords = new HashSet<>();
        StringBuilder textBuilder = new StringBuilder();
        if (info.getSupportTarget() != null) textBuilder.append(info.getSupportTarget()).append(" ");
        if (info.getServiceName() != null) textBuilder.append(info.getServiceName()).append(" ");
        if (detail.getSupportTarget() != null) textBuilder.append(detail.getSupportTarget()).append(" ");
        if (info.getUserCategory() != null) textBuilder.append(info.getUserCategory()).append(" ");
        String combinedText = textBuilder.toString();

        if (combinedText.contains("소상공인") || combinedText.contains("자영업자") || combinedText.contains("소기업")) keywords.add("소상공인/자영업자");
        if (condition.getBusinessProspective() != null || combinedText.contains("예비창업")) keywords.add("예비 창업자");
        if (combinedText.contains("청년")) keywords.add("청년");
        if (combinedText.contains("전통시장") || combinedText.contains("상점가")) keywords.add("전통시장/상점가");
        if (combinedText.contains("농업") || combinedText.contains("어업") || combinedText.contains("축산")) keywords.add("농축수산업");
        if (combinedText.contains("제조업") || combinedText.contains("소공인")) keywords.add("제조업/소공인");
        if (combinedText.contains("경영위기") || combinedText.contains("폐업") || combinedText.contains("재기") || combinedText.contains("재도전")) keywords.add("경영위기/재창업");
        if (combinedText.contains("여성") || combinedText.contains("장애인") || combinedText.contains("다문화")) keywords.add("여성/장애인/다문화");
        if (combinedText.contains("국민") || combinedText.contains("누구나")) keywords.add("일반 국민/개인");

        String locationText = info.getDepartmentName();
        if (locationText != null) {
            Pattern pattern = Pattern.compile("(\\S+[시도])?\\s*(\\S+[군구])");
            Matcher matcher = pattern.matcher(locationText);
            if (matcher.find()) {
                if (matcher.group(1) != null) keywords.add(matcher.group(1).trim());
                if (matcher.group(2) != null) keywords.add(matcher.group(2).trim());
            }
        }

        return String.join(",", keywords);
    }
}
package com.onmarket.supportsdata.service;

import com.onmarket.supportsdata.domain.SupportCondition;
import com.onmarket.supportsdata.domain.SupportService;
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
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicDataServiceImpl implements PublicDataService {

    private final WebClient.Builder webClientBuilder;
    private final SupportServiceRepository supportServiceRepository;

    @Value("${gov.api.base-url}")
    private String baseUrl;

    @Value("${gov.api.service-key}")
    private String serviceKey;


    @Override
    @Transactional
    public Mono<Void> findAndSaveAllServices() {
        final int perPage = 100;
        WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();

        // ▼▼▼ 여기에 필터링할 키워드를 정의합니다. (API 공식 문서에서 정확한 값 확인 필요) ▼▼▼
        final String userCategory = "소상공인"; // "소상공인"이 안될 경우 "자영업자", "중소기업" 등으로 시도

        // getAllServiceInfoDTOs를 호출할 때 userCategory를 전달합니다.
        return getAllServiceInfoDTOs(webClient, perPage, userCategory)
                .delayElements(Duration.ofMillis(100))
                .flatMap(serviceInfo -> {
                    // ... (이하 flatMap 내부는 기존과 동일하게 유지)
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

    // getAllServiceInfoDTOs 메소드 시그니처에 userCategory 파라미터를 추가합니다.
    private Flux<ServiceInfoDTO> getAllServiceInfoDTOs(WebClient webClient, int perPage, String userCategory) {
        // 첫 페이지 호출 시 userCategory를 전달합니다.
        return callApi(webClient, 1, perPage, userCategory, new ParameterizedTypeReference<ApiResponseDTO<ServiceInfoDTO>>() {})
                .expand(response -> {
                    int totalCount = response.getTotalCount();
                    int currentPage = response.getPage();
                    int totalPages = (int) Math.ceil((double) totalCount / perPage);
                    if (currentPage < totalPages) {
                        // 다음 페이지 호출 시에도 userCategory를 계속 전달합니다.
                        return callApi(webClient, currentPage + 1, perPage, userCategory, new ParameterizedTypeReference<ApiResponseDTO<ServiceInfoDTO>>() {});
                    } else {
                        return Mono.empty();
                    }
                })
                .flatMapIterable(response -> Optional.ofNullable(response.getData()).orElse(Collections.emptyList()));
    }

    // callApi 메소드 시그니처에 userCategory 파라미터를 추가하고, uriBuilder에 필터 조건을 적용합니다.
    private <T> Mono<ApiResponseDTO<T>> callApi(WebClient webClient, int page, int perPage, String userCategory, ParameterizedTypeReference<ApiResponseDTO<T>> typeReference) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/gov24/v3/serviceList")
                        .queryParam("page", page)
                        .queryParam("perPage", perPage)
                        .queryParam("cond[사용자구분::LIKE]", userCategory) // ◀◀-- 필터 조건 적용
                        .queryParam("serviceKey", serviceKey)
                        .build())
                .retrieve()
                .bodyToMono(typeReference)
                .doOnError(WebClientResponseException.class, err -> log.error("API Error - Status: [{}], Page: [{}], Body: {}", err.getStatusCode(), page, err.getResponseBodyAsString()));
    }

    private Flux<ServiceInfoDTO> getAllServiceInfoDTOs(WebClient webClient, int perPage) {
        return callApi(webClient, 1, perPage, new ParameterizedTypeReference<ApiResponseDTO<ServiceInfoDTO>>() {})
                .expand(response -> {
                    int totalCount = response.getTotalCount();
                    int currentPage = response.getPage();
                    int totalPages = (int) Math.ceil((double) totalCount / perPage);
                    if (currentPage < totalPages) {
                        return callApi(webClient, currentPage + 1, perPage, new ParameterizedTypeReference<ApiResponseDTO<ServiceInfoDTO>>() {});
                    } else {
                        return Mono.empty();
                    }
                })
                .flatMapIterable(response -> Optional.ofNullable(response.getData()).orElse(Collections.emptyList()));
    }

    private <T> Mono<ApiResponseDTO<T>> callApi(WebClient webClient, int page, int perPage, ParameterizedTypeReference<ApiResponseDTO<T>> typeReference) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/gov24/v3/serviceList")
                        .queryParam("page", page)
                        .queryParam("perPage", perPage)
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
                        .queryParam("cond[서비스ID::EQ]", serviceId) // 상세 API는 서비스ID로 필터링
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {});
    }

    private Mono<ApiResponseDTO<SupportConditionDTO>> callConditionApi(WebClient webClient, String serviceId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/gov24/v3/supportConditions")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("cond[서비스ID::EQ]", serviceId) // 지원조건 API도 서비스ID로 필터링
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {});
    }

    // PublicDataServiceImpl.java

    private void createAndSaveEntities(ServiceInfoDTO infoDTO, ServiceDetailDTO detailDTO, SupportConditionDTO conditionDTO) {
        // SupportService 엔티티 생성
        SupportService serviceEntity = SupportService.builder()
                .serviceId(infoDTO.getServiceId())
                .supportType(infoDTO.getSupportType())
                .serviceName(infoDTO.getServiceName())
                .servicePurposeSummary(infoDTO.getServicePurposeSummary())
                .supportTarget(infoDTO.getSupportTarget())
                .selectionCriteria(infoDTO.getSelectionCriteria())
                .supportContent(infoDTO.getSupportContent())
                .applicationMethod(infoDTO.getApplicationMethod())
                .detailUrl(infoDTO.getDetailUrl())
                .departmentName(infoDTO.getDepartmentName())
                .servicePurpose(detailDTO.getServicePurpose())
                .applicationDeadline(detailDTO.getApplicationDeadline())
                .requiredDocuments(detailDTO.getRequiredDocuments())
                .receptionAgencyName(detailDTO.getReceptionAgencyName())
                .contact(detailDTO.getContact())
                .onlineApplicationUrl(detailDTO.getOnlineApplicationUrl())
                .laws(detailDTO.getLaws())
                .build();

        // SupportCondition 엔티티 생성
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
                // ... 나머지 conditionDTO 필드들도 모두 추가 ...
                .build();

        // 연관관계를 설정하고 DB에 저장
        conditionEntity.setSupportService(serviceEntity);
        supportServiceRepository.save(serviceEntity);
    }
}
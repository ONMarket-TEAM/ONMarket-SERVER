package com.onmarket.loandata.service;

import com.onmarket.loandata.domain.LoanProductEntity;

import com.onmarket.loandata.dto.LoanApiResponse;
import com.onmarket.loandata.dto.LoanProduct;
import com.onmarket.loandata.repository.LoanProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanDataServiceImpl implements LoanDataService {

    private final WebClient.Builder webClientBuilder;
    private final LoanProductRepository loanProductRepository;

    @Value("${gov.api.loan.base-url}")
    private String baseUrl;

    @Value("${gov.api.loan.service-key}")
    private String serviceKey;

    @Override
    @Transactional
    public Mono<Void> fetchAndSaveLoanProducts() {
        WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();
        final int numOfRows = 100;

        return callLoanApi(webClient, 1, numOfRows)
                .expand(response -> {
                    if (response.getBody() == null || response.getBody().getItems() == null || response.getBody().getItems().getItemList() == null) {
                        return Mono.empty();
                    }
                    int totalCount = response.getBody().getTotalCount();
                    int currentPage = response.getBody().getPageNo();
                    if (totalCount == 0 || currentPage * numOfRows >= totalCount) {
                        return Mono.empty();
                    }
                    return callLoanApi(webClient, currentPage + 1, numOfRows);
                })
                .flatMapIterable(response -> Optional.ofNullable(response.getBody().getItems().getItemList()).orElse(Collections.emptyList()))
                .filter(dto -> !loanProductRepository.existsById(dto.getSeq())) // 중복 데이터 건너뛰기
                .map(this::convertDtoToEntity)
                .buffer(100)
                .doOnNext(loanProductRepository::saveAll)
                .then();
    }

    private Mono<LoanApiResponse> callLoanApi(WebClient webClient, int pageNo, int numOfRows) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getLoanProductSearchingInfo")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("type", "json")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(LoanApiResponse.class)
                .doOnError(e -> log.error("API call failed for page {}: {}", pageNo, e.getMessage()));
    }

    private LoanProductEntity convertDtoToEntity(LoanProduct dto) {
        return LoanProductEntity.builder()
                .seq(dto.getSeq())
                .productName(dto.getProductName())
                .handlingInstitution(dto.getHandlingInstitution())
                .interestRateType(dto.getInterestRateType())
                .interestRate(dto.getInterestRate())
                .loanLimit(dto.getLoanLimit())
                .maxLoanTerm(dto.getMaxLoanTerm())
                .loanPurpose(dto.getLoanPurpose())
                .supportTargetDetail(dto.getSupportTargetDetail())
                .build();
    }
}
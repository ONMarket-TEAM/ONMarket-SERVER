package com.onmarket.loandata.service;

import reactor.core.publisher.Mono;

public interface LoanDataService {
    Mono<Void> fetchAndSaveLoanProducts();
}
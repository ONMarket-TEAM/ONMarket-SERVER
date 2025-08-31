package com.onmarket.supportsdata.service;

import reactor.core.publisher.Mono;

public interface PublicDataService {

    Mono<Void> findAndSaveAllServices();
}
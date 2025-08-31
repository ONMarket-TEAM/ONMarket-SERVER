package com.onmarket.supportsdata.service;

import reactor.core.publisher.Mono;

public interface PublicDataService {

    Mono<Void> findAndSaveAllServices(); // ◀◀-- 이름과 반환 타입을 이렇게 변경
}
package com.onmarket.supportsdata.service;

import reactor.core.publisher.Mono;

public interface PublicDataService {

    /**
     * 모든 공공서비스 데이터를 가져와 DB에 저장합니다.
     * 작업 완료 신호만 반환합니다.
     */
    Mono<Void> findAndSaveAllServices(); // ◀◀-- 이름과 반환 타입을 이렇게 변경
}
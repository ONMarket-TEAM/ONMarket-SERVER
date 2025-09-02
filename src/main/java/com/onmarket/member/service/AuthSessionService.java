package com.onmarket.member.service;

public interface AuthSessionService {

    /**
     * 프로필 수정용 임시 인증 토큰 생성
     */
    String createAuthSession(String email);

    /**
     * 인증 토큰 검증 및 이메일 반환
     */
    String validateAuthSession(String sessionToken);

    /**
     * 인증 토큰 무효화
     */
    void invalidateAuthSession(String sessionToken);
}

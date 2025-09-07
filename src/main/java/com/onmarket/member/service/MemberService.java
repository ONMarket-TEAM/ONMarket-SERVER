package com.onmarket.member.service;

import com.onmarket.member.domain.Member;
import com.onmarket.member.dto.*;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

public interface MemberService {

    /**
     * 이메일로 회원 조회
     */
    Member findByEmail(String email);

    /**
     * ID로 회원 조회
     */
    Member findById(Long memberId);

    /**
     * 회원 탈퇴
     */
    void withdraw(String token);

    /**
     * 현재 비밀번호 일치 여부 확인(이메일 기반)
     */
    void verifyPassword(String email, String currentPassword);

    /**
     * 회원정보 수정(이메일 기반)
     */
    Member updateMember(String email, MemberUpdateRequest request);

    /**
     * 이름/휴대폰 기반으로 이메일 찾기
     */
    String findId(String userName, String phone);

    /**
     * 회원 저장
     */
    Member save(Member member);

    /**
     * 소셜 로그인 통합 처리 (카카오, 구글 등)
     * - 기존 회원: 상태에 따라 로그인 또는 추가정보 입력 필요
     * - 신규 회원: PENDING 상태로 생성 후 추가정보 입력 필요
     */
    SocialLoginResponse handleSocialLogin(SocialUserInfo info);

    /**
     * 소셜 회원가입 완료 (추가 정보 입력 후 ACTIVE 상태로 변경)
     */
    public CompleteSocialSignupResponse completeSocialSignup(Long memberId, String nickname, String profileImageKey );
}
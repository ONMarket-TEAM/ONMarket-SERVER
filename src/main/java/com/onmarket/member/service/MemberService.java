package com.onmarket.member.service;

import com.onmarket.member.domain.Member;
import com.onmarket.member.dto.MemberUpdateRequest;

public interface MemberService {

    /** 이메일로 회원 조회 */
    Member findByEmail(String email);

    /** ID로 회원 조회 */
    Member findById(Long memberId);

    void withdraw(String token);


    /** 현재 비밀번호 일치 여부 확인(이메일 기반) */
    void verifyPassword(String email, String currentPassword);

    /** 회원정보 수정(이메일 기반) */
    Member updateMember(String email, MemberUpdateRequest request);

    /** 이메일/휴대폰 기반으로 아이디 찾기 */
    String findId(String userName, String phone);
}

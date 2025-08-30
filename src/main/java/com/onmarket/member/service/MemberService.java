package com.onmarket.member.service;

import com.onmarket.member.domain.Member;

public interface MemberService {

    /** 이메일로 회원 조회 */
    Member findByEmail(String email);

    /** ID로 회원 조회 */
    Member findById(Long memberId);

    void withdraw(String token);

}

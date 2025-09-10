package com.onmarket.member.repository;

import com.onmarket.member.domain.Member;
import com.onmarket.member.domain.enums.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByNickname(String nickname);
    boolean existsByEmail(String email);
    Optional<Member> findByEmail(String email);
    Optional<Member> findByUsernameAndPhone(String username, String phone);
    Optional<Member> findBySocialIdAndSocialProvider(String socialId, SocialProvider socialProvider);

    boolean existsByEmailAndMemberIdNot(String email, Long memberId);
    boolean existsByNicknameAndMemberIdNot(String nickname, Long memberId);

}

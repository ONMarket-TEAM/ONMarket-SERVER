package com.onmarket.member.dto;

import com.onmarket.member.domain.Member;
import lombok.Getter;

@Getter
public class MemberResponse {
    private final Long memberId;
    private final String username;
    private final String nickname;
    private final String email;

    public MemberResponse(Long memberId, String username, String nickname, String email) {
        this.memberId = memberId;
        this.username = username;
        this.nickname = nickname;
        this.email = email;
    }

    public static MemberResponse from(Member m) {
        return new MemberResponse(
                m.getMemberId(),
                m.getUsername(),
                m.getNickname(),
                m.getEmail()
        );
    }
}

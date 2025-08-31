package com.onmarket.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberUpdateRequest {
    private String nickname;            // 닉네임
    private String currentPassword;     // 비번 변경 시 필수
    private String newPassword;         // 새 비밀번호
    private String confirmNewPassword;  // 새 비밀번호 확인
}

package com.onmarket.member.dto;

import com.onmarket.member.domain.enums.Gender;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SignupRequest {
    private String username;       // 로그인 아이디
    private String password;       // 비밀번호
    private String nickname;       // 닉네임
    private String email;          // 이메일
    private String phone;          // 전화번호
    private String profileImage;   // 프로필 이미지 URL
    private LocalDate birthDate;   // 생년월일 (yyyy-MM-dd)
    private Gender gender;         // 성별 ENUM (MALE, FEMALE, OTHER)
    private Long mainBusinessId;   // 주 사업장 ID
}

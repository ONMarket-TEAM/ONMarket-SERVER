package com.onmarket.member.dto;

import com.onmarket.member.domain.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SignupRequest {

    @Schema(description = "이름", example = "홍길동")
    private String username;

    @Schema(description = "비밀번호 (8자 이상, 영문/숫자/특수문자 포함)", example = "123456789!")
    private String password;

    @Schema(description = "사용자 닉네임", example = "testuser")
    private String nickname;

    @Schema(description = "이메일 주소", example = "test@test.com")
    private String email;

    @Schema(description = "전화번호 ('-' 포함 가능)", example = "010-1234-5678")
    private String phone;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    private String profileImage;

    @Schema(description = "생년월일 (yyyy-MM-dd 형식)", example = "1995-08-27", type = "string")
    private LocalDate birthDate;

    @Schema(description = "성별 (MALE, FEMALE, OTHER)", example = "MALE")
    private Gender gender;

    @Schema(description = "주 사업장 ID", example = "1")
    private Long mainBusinessId;
}

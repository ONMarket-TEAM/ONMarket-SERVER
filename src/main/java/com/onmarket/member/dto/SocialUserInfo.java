package com.onmarket.member.dto;

import com.onmarket.common.response.ResponseCode;
import com.onmarket.member.domain.enums.Gender;
import com.onmarket.business.exception.BusinessException;
import com.onmarket.member.domain.enums.SocialProvider;
import lombok.*;

import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SocialUserInfo {
    private String socialId;
    private String email;
    private String nickname;
    private String profileImage;
    private String realName;
    private LocalDate birthDate;
    private String phoneNumber;
    private Gender gender;
    private SocialProvider socialProvider;

    @SuppressWarnings("unchecked")
    public static SocialUserInfo fromKakao(OAuth2User oAuth2User) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
        if (kakaoAccount == null) {
            throw new BusinessException(ResponseCode.OAUTH2_LOGIN_FAILED);
        }

        String socialId = String.valueOf(oAuth2User.getAttributes().get("id"));
        String email = (String) kakaoAccount.get("email");
        String realName = (String) kakaoAccount.get("name");
        String birthday = (String) kakaoAccount.get("birthday");
        String birthyear = (String) kakaoAccount.get("birthyear");
        String phone = (String) kakaoAccount.get("phone_number");
        String genderStr = (String) kakaoAccount.get("gender");

        // Profile
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        String nickname = profile != null ? (String) profile.get("nickname") : null;
        String profileImage = profile != null ? (String) profile.get("profile_image_url") : null;

        // 생년월일
        LocalDate birthDate = null;
        if (birthday != null && birthyear != null) {
            try {
                birthDate = LocalDate.parse(birthyear + "-" + birthday.substring(0, 2) + "-" + birthday.substring(2, 4));
            } catch (Exception ignored) {}
        }

        // Gender
        Gender gender = switch (genderStr != null ? genderStr.toLowerCase() : "") {
            case "male" -> Gender.MALE;
            case "female" -> Gender.FEMALE;
            default -> Gender.OTHER;
        };

        // 전화번호 보정
        String phoneNumber = (phone != null) ? "0" + phone.split(" ")[1] : null;

        return SocialUserInfo.builder()
                .socialId(socialId)
                .email(email)
                .nickname(nickname)
                .profileImage(profileImage)
                .realName(realName)
                .birthDate(birthDate)
                .phoneNumber(phoneNumber)
                .gender(gender)
                .socialProvider(SocialProvider.KAKAO)
                .build();
    }
}

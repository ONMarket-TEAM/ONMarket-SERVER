package com.onmarket.member.domain;

import com.onmarket.common.domain.BaseTimeEntity;
import com.onmarket.member.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider", length = 20)
    private SocialProvider socialProvider;

    @Column(name = "social_id", length = 100, unique = true)
    private String socialId;

    @Column(name = "username", length = 50, nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;  // 비밀번호 (암호화)

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "profile_image", length = 255)
    private String profileImage;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Column(name = "main_business_id")
    private Long mainBusinessId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private MemberStatus status;

    @Column(name = "refresh_token", length = 512)
    private String refreshToken;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) this.status = MemberStatus.ACTIVE;
    }

    /** 닉네임 변경 */
    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    /** 비밀번호 변경 */
    public void changePassword(String password) {
        this.password = password;
    }

    /** 프로필 이미지 변경 */
    public void changeProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    /** 리프레시 토큰 갱신 */
    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /** 회원 상태 변경  */
    public void changeStatus(MemberStatus status) {
        this.status = status;
    }
}

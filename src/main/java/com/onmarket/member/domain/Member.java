package com.onmarket.member.domain;

import com.onmarket.common.domain.BaseTimeEntity;
import com.onmarket.member.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    // 소셜 로그인은 나중에 사용 → nullable
    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider", length = 20)
    private SocialProvider socialProvider;

    @Column(name = "social_id", length = 100, unique = true)
    private String socialId;

    // 기본 회원가입 필드
    @Column(name = "username", length = 50, nullable = false)
    private String username;  // 로그인 아이디(고유값)

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
}

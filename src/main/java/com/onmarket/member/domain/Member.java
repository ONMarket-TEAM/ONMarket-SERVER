package com.onmarket.member.domain;

import com.onmarket.business.domain.Business;
import com.onmarket.common.domain.BaseTimeEntity;
import com.onmarket.member.domain.enums.*;
import com.onmarket.member.dto.SocialUserInfo;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "instagram_username", length = 100)
    private String instagramUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private Role role;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) this.status = MemberStatus.ACTIVE;
        if (this.role == null) this.role = Role.USER;  // 기본값 USER
    }

    @OneToMany(mappedBy = "member", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Business> businesses = new ArrayList<>();

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

    /** Instagram 계정명 변경/삭제 */
    public void changeInstagramUsername(String username) {
        this.instagramUsername = username;
    }

    /** Instagram 연결 여부 확인 */
    public boolean hasInstagramConnected() {
        return this.instagramUsername != null && !this.instagramUsername.trim().isEmpty();
    }


    /** 표시용 Instagram 계정명 반환 (@ 포함) */
    public String getDisplayInstagramUsername() {
        if (this.instagramUsername == null || this.instagramUsername.trim().isEmpty()) {
            return null;
        }
        // @ 가 없으면 추가, 있으면 그대로
        return this.instagramUsername.startsWith("@") ?
                this.instagramUsername : "@" + this.instagramUsername;
    }
    /** 역할 변경 */
    public void changeRole(Role role) {
        this.role = role;
        this.refreshToken = null;
    }

    /** 소셜 로그인용 PENDING 상태로 Member 생성 */
    public static Member createSocialPendingMember(SocialUserInfo socialInfo) {
        return Member.builder()
                .socialProvider(socialInfo.getSocialProvider())
                .socialId(socialInfo.getSocialId())
                .email(socialInfo.getEmail())
                .nickname(socialInfo.getNickname())
                .username(socialInfo.getEmail())
                .password("SOCIAL_LOGIN")
                .birthDate(socialInfo.getBirthDate())
                .phone(socialInfo.getPhoneNumber())
                .gender(socialInfo.getGender())
                .status(MemberStatus.PENDING)
                .role(Role.USER)
                .build();
    }
    public void completeSignup(String nickname) {
        this.nickname = nickname;
        this.status = MemberStatus.ACTIVE;
    }

    /** PENDING 상태인지 확인 */
    public boolean isPending() {
        return this.status == MemberStatus.PENDING;
    }

    /** ACTIVE 상태인지 확인 */
    public boolean isActive() {
        return this.status == MemberStatus.ACTIVE;
    }
}

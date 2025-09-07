package com.onmarket.oauth.handler;

import com.onmarket.member.domain.enums.MemberStatus;
import com.onmarket.member.dto.SocialLoginResponse;
import com.onmarket.member.dto.SocialUserInfo;
import com.onmarket.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final MemberService memberService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

            // 어떤 소셜 로그인인지 확인
            String registrationId = ((org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) authentication)
                    .getAuthorizedClientRegistrationId();

            // provider 별로 user info 파싱
            SocialUserInfo info;
            if ("kakao".equals(registrationId)) {
                info = SocialUserInfo.fromKakao(oAuth2User);
            } else if ("google".equals(registrationId)) {
                info = SocialUserInfo.fromGoogle(oAuth2User);
            } else {
                throw new IllegalArgumentException("Unsupported provider: " + registrationId);
            }

            // 회원 처리
            SocialLoginResponse result = memberService.handleSocialLogin(info);

            if (result.getStatus() == MemberStatus.PENDING) {
                response.sendRedirect("http://localhost:5173/login/signup?memberId=" + result.getMemberId());
            } else {
                response.sendRedirect("http://localhost:5173?accessToken=" + result.getAccessToken() +
                        "&refreshToken=" + result.getRefreshToken());
            }
        } catch (Exception e) {
            response.sendRedirect("http://localhost:5173/login?error=oauth_failed");
        }
    }

}
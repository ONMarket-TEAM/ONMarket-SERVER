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
            SocialUserInfo info = SocialUserInfo.fromKakao(oAuth2User);
            SocialLoginResponse result = memberService.handleSocialLogin(info);

            if (result.getStatus() == MemberStatus.PENDING) {
                // 회원가입 페이지로 이동 (약관 동의부터 시작)
                response.sendRedirect("http://localhost:5173/login/signup?memberId=" + result.getMemberId());
            } else {
                // 로그인 완료 - 메인 페이지로 토큰과 함께 이동
                response.sendRedirect("http://localhost:5173?accessToken=" + result.getAccessToken() +
                        "&refreshToken=" + result.getRefreshToken());
            }
        } catch (Exception e) {
            response.sendRedirect("http://localhost:5173/login?error=oauth_failed");
        }
    }
}
package com.onmarket.oauth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    // ✅ 이 경로들은 인증필터를 아예 적용하지 않음
    private static final String[] EXCLUDE_PATHS = {
            "/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**",
            "/swagger-resources/**", "/webjars/**",
            "/api/signup", "/api/auth/**", "/api/validation/**",
            "/api/members/find-id", "/api/sms/**", "/api/s3/**",
            "/api/support-products/**", "/api/loan-products/**", "/api/credit-loans/**",
            "/api/cardnews/**"   // ⬅️ 카드뉴스 공개
    };
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /** 공개 경로는 필터 스킵 */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String p : EXCLUDE_PATHS) {
            if (PATH_MATCHER.match(p, uri)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // ⭐ 토큰이 없으면 그냥 통과 (permitAll 경로 + 인증 불필요 경로 지원)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // 토큰 유효성 검사
        if (!jwtTokenProvider.validateToken(token)) {
            // 보호된 엔드포인트에서만 401이 의미 있음. 여기서 바로 끊어도 됨.
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token");
            return;
        }

        // 토큰이 유효하면 SecurityContext에 인증정보 저장
        String email = jwtTokenProvider.getEmail(token);
        var authentication = new UsernamePasswordAuthenticationToken(email, null, null);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
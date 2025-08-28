package com.onmarket.common.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Swagger, API docs, Webjars 요청은 필터 제외
        if (path.startsWith("/swagger-ui")
                || path.startsWith("/api-docs")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars")) {
            filterChain.doFilter(request, response);
            return;
        }

        // JWT 인증 처리
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // 토큰 유효성 검사
            if (!jwtTokenProvider.validateToken(token)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token");
                return;
            }

            // 토큰이 유효하다면 SecurityContext에 인증 정보 저장
            String email = jwtTokenProvider.getEmail(token);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(email, null, null);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}

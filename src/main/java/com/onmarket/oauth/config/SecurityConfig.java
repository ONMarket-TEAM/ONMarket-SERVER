package com.onmarket.oauth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onmarket.common.response.ApiResponse;
import com.onmarket.member.dto.SocialUserInfo;
import com.onmarket.oauth.handler.CustomOAuth2SuccessHandler;
import com.onmarket.oauth.jwt.JwtAuthenticationFilter;
import com.onmarket.oauth.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Configuration
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CustomOAuth2SuccessHandler successHandler) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/index.html",
                                "/swagger-ui-onmarket.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/error"
                        ).permitAll()

                        // --- Web Push 테스트용 공개 엔드포인트
                        .requestMatchers(HttpMethod.GET,  "/api/push/vapidPublicKey").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/push/subscribe").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/push/send").permitAll()   // 개발 중에만 오픈

                        // --- 정적 파일(서비스워커 등)
                        .requestMatchers("/sw.js", "/favicon.ico", "/manifest.json").permitAll()

                        // --- CORS 프리플라이트
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers(
                                "/api/signup",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/validation/check/*",
                                "/api/members/find-id",
                                "/api/sms/*",
                                "/api/s3/**",
                                "/api/auth/email/**",
                                "/api/support-products/*",
                                "/api/loan-products/*",
                                "/api/credit-loans/*",
                                "/api/captions/**",
                                "/api/summary/**",
                                "/login/oauth2/code/**",
                                "/oauth2/authorization/**",
                                "/api/oauth/**",
                                "/api/posts/**",
                                "/api/posts/type/**",
                                "/api/posts/generate",
                                "/api/cardnews/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .successHandler(successHandler)
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"code\":\"OAUTH2_LOGIN_FAILED\",\"message\":\""
                                            + exception.getMessage() + "\"}"
                            );
                        })
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
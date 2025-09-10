package com.onmarket.oauth.config;

import com.onmarket.oauth.handler.CustomOAuth2SuccessHandler;
import com.onmarket.oauth.jwt.JwtAuthenticationFilter;
import com.onmarket.oauth.jwt.JwtTokenProvider;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.NullSecurityContextRepository; // ★ 추가
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List; // ★ 추가

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

                // ★ 완전 무상태 + 세션ID 변경 비활성화
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .sessionFixation(sf -> sf.none())
                )

                // ★ SecurityContext 를 세션/응답에 저장하지 않음 (Spring Session 저장 트리거 차단)
                .securityContext(sc -> sc.securityContextRepository(new NullSecurityContextRepository()))

                // ★ 불필요한 기본 인증 필터들 제거
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

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

                        // Web Push 테스트용 공개 엔드포인트
                        .requestMatchers(HttpMethod.GET,  "/api/push/vapidPublicKey").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/push/subscribe").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/push/send").permitAll()

                        // 정적 파일(서비스워커 등)
                        .requestMatchers("/sw.js", "/favicon.ico", "/manifest.json").permitAll()

                        // CORS 프리플라이트
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 공개 API
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

                // OAuth2 로그인은 유지(성공 시 JWT 발급)
                .oauth2Login(oauth -> oauth
                        .successHandler(successHandler)
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"code\":\"OAUTH2_LOGIN_FAILED\",\"message\":\""
                                            + exception.getMessage() + "\"}"
                            );
                        })
                )

                // JWT 필터 추가 (매 요청 한 번만 동작하도록 OncePerRequestFilter 구현체 사용)
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ★ allowCredentials=true를 쓸 때는 origin을 명시적으로 지정해야 함(* 금지)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));
        configuration.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

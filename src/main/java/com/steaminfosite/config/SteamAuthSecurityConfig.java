package com.steaminfosite.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SteamAuthSecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain steamAuthSecurityFilterChain(HttpSecurity http)
            throws Exception {
        CookieCsrfTokenRepository csrfRepository = new CookieCsrfTokenRepository();

        csrfRepository.setCookieCustomizer(cookie -> cookie
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/api/auth/steam"));

        http.securityMatcher("/api/auth/steam/**")
                .authorizeHttpRequests(authorize ->
                        authorize.anyRequest().permitAll())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .requireCsrfProtectionMatcher(request -> {
                            String path = request.getRequestURI();
                            String base = request.getContextPath() + "/api/auth/steam/";

                            return "POST".equals(request.getMethod())
                                    && (path.equals(base + "refresh")
                                    || path.equals(base + "logout"));
                        }));

        return http.build();
    }
}
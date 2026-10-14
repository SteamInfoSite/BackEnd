package com.steaminfosite.config;

import com.steaminfosite.controller.SteamAuthController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommunitySecurityConfig {

    @Bean
    SecurityFilterChain communitySecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/community/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(new HttpSessionCsrfTokenRepository())
                        .requireCsrfProtectionMatcher(CommunitySecurityConfig::requiresCsrf));

        return http.build();
    }

    private static boolean requiresCsrf(HttpServletRequest request) {
        String method = request.getMethod();
        if (method.equals("GET") || method.equals("HEAD")
                || method.equals("OPTIONS") || method.equals("TRACE")) {
            return false;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        Object steamId = session.getAttribute(SteamAuthController.STEAM_ID_SESSION_KEY);
        return steamId instanceof String id && !id.isBlank();
    }
}

package org.example.winefinder.config;

import org.example.winefinder.auth.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        // PBKDF2는 긴 UTF-8 비밀번호도 잘리지 않으며 사용자마다 무작위 salt를 사용합니다.
        return new org.springframework.security.crypto.password.DelegatingPasswordEncoder("pbkdf2@SpringSecurity_v5_8",
                java.util.Map.of("pbkdf2@SpringSecurity_v5_8",
                        org.springframework.security.crypto.password.Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()));
    }

    @Bean
    UserDetailsService userDetailsService(MemberRepository members) {
        return email -> members.findByEmail(AuthService.normalizeEmail(email))
                .map(m -> User.withUsername(m.getEmail()).password(m.getPasswordHash()).roles("USER").build())
                .orElseThrow(() -> new UsernameNotFoundException("이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.cors(cors -> {})
                .authorizeHttpRequests(r -> r
                        .requestMatchers("/error", "/api/auth/csrf", "/api/auth/signup", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/items", "/api/items/*", "/api/home", "/api/images/*", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(f -> f.loginProcessingUrl("/api/auth/login").usernameParameter("email")
                        .successHandler((req, res, a) -> res.setStatus(204))
                        .failureHandler((req, res, e) -> {
                            res.setStatus(401); res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"message\":\"이메일 또는 비밀번호가 올바르지 않습니다.\"}");
                        }).permitAll())
                .logout(l -> l.logoutUrl("/api/auth/logout").invalidateHttpSession(true).deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((req, res, a) -> res.setStatus(204)))
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401); res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"message\":\"로그인이 필요합니다.\"}");
                }).accessDeniedHandler((req, res, ex) -> {
                    res.setStatus(403); res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"message\":\"권한 또는 CSRF 토큰을 확인해 주세요.\"}");
                }))
                .build();
    }
}

package com.cbnuccc.cbnuccc.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.cbnuccc.cbnuccc.Filter.JwtFilter;
import com.cbnuccc.cbnuccc.Util.SecurityUtil;

@Configuration
public class SecurityConfig {
    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf((csrf) -> csrf.disable());
        http.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        // Spring Security의 기본 "/logout" 처리(세션 기반 리다이렉트)를 비활성화하고
        // RefreshController의 커스텀 "/logout" 엔드포인트가 요청을 처리하도록 함
        http.logout((logout) -> logout.disable());
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        http.authorizeHttpRequests(auth -> {
            // 인증 없이 통과할 수 있는 리스트 중 하나에 해당되면 통과
            SecurityUtil.EXCLUDE_LIST.forEach(exclude -> {
                auth.requestMatchers(
                        exclude.method(),
                        exclude.uriPattern()).permitAll();
            });

            // 사역팀 순장(2), 간사(4) Role 요구
            auth.requestMatchers(HttpMethod.GET, "/stc/excel").hasAnyRole("2", "4");

            // 그 외의 URI에 대해 인증 필요
            auth.anyRequest().authenticated();
        });

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}

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
import com.cbnuccc.cbnuccc.Filter.RequestLoggingFilter;
import com.cbnuccc.cbnuccc.Util.SecurityUtil;

@Configuration
public class SecurityConfig {
    private final JwtFilter jwtFilter;
    private final RequestLoggingFilter requestLoggingFilter;

    public SecurityConfig(JwtFilter jwtFilter, RequestLoggingFilter requestLoggingFilter) {
        this.jwtFilter = jwtFilter;
        this.requestLoggingFilter = requestLoggingFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf((csrf) -> csrf.disable());
        http.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        // Spring Security의 기본 "/logout" 처리(세션 기반 리다이렉트)를 비활성화하고
        // RefreshController의 커스텀 "/logout" 엔드포인트가 요청을 처리하도록 함
        http.logout((logout) -> logout.disable());
        // JwtFilter를 먼저 등록해 그 클래스의 순서를 Spring Security에 알려준 뒤,
        // 요청 로깅 필터를 그보다 앞에 두어 모든 요청(인증 제외 대상 포함)을 감싸도록 함
        // (JwtFilter의 순서가 등록되기 전에 그 클래스를 기준으로 addFilterBefore를 호출하면
        // "does not have a registered order" 예외가 발생하므로 반드시 이 순서로 호출해야 함)
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(requestLoggingFilter, JwtFilter.class);

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

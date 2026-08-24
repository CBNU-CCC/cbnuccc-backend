package com.cbnuccc.cbnuccc.Filter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.cbnuccc.cbnuccc.Model.MyUser;
import com.cbnuccc.cbnuccc.Repository.UserJpaRepository;
import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.SecurityUtil;
import com.cbnuccc.cbnuccc.Util.StatusCode;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {
    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private UserJpaRepository userJpaRepository;

    private static final AntPathMatcher matcher = new AntPathMatcher();

    // 두 번 실행되지 않도록 처리하기
    @Bean
    public FilterRegistrationBean<JwtFilter> disableJwtFilter(JwtFilter filter) {
        FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    // 필터링에서 제외할 대상인지 확인하기
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        HttpMethod requestMethod = HttpMethod.valueOf(method);

        // 로그 출력을 위한 설정
        MDC.put("endpoint", requestUri);
        MDC.put("method", method);
        MDC.put("ip", SecurityUtil.getClientIp(request));

        boolean result = SecurityUtil.EXCLUDE_LIST.stream()
                .anyMatch(exclude -> exclude.method() == requestMethod &&
                        matcher.match(exclude.uriPattern(), requestUri));
        if (result)
            LogUtil.printBasicInfoLog(LogHeader.ENTER, (Object[]) null);
        return result;
    }

    // 필터링하기
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // jwt 토큰을 가져오기 위해 Authorization 헤더 가져오기
        String authString = request.getHeader("Authorization");
        Optional<String> _jwtToken = securityUtil.getAuthorizationToken(authString);
        if (_jwtToken == null) {
            response.sendError(
                    StatusCode.INVALID_TOKEN.getResponseStatus().value(),
                    StatusCode.INVALID_TOKEN.getErrorMessage());
            return;
        }
        String jwtToken = _jwtToken.get();

        // claim을 가져오기 위해 주어진 토큰 추출하기
        Claims claim;
        try {
            claim = securityUtil.extractToken(jwtToken);
        } catch (Exception e) {
            response.sendError(
                    StatusCode.INVALID_TOKEN.getResponseStatus().value(),
                    StatusCode.INVALID_TOKEN.getErrorMessage());
            return;
        }

        // 로그 출력 시 'entered_user_uuid' 속성이 함께 출력되도록 추가하기
        String uuidString = claim.get("uuid").toString();
        MDC.put("entered_user_uuid", uuidString.substring(0, 8));

        // 로그 출력 시 'entered_user_email' 속성이 함께 출력되도록 추가하기
        UUID uuid = UUID.fromString(uuidString);
        Optional<MyUser> _enteredUser = userJpaRepository.findByUuid(uuid);
        if (_enteredUser.isPresent()) {
            MyUser enteredUser = _enteredUser.get();
            MDC.put("entered_user_email", String.valueOf(enteredUser.getEmail().toLowerCase().hashCode()));
        }

        // 진입 로그 출력하기
        LogUtil.printBasicInfoLog(LogHeader.ENTER, (Object[]) null);

        // 로그인을 위한 최종 설정
        List<SimpleGrantedAuthority> roles = List.of(new SimpleGrantedAuthority("ROLE_" + claim.get("rank")));
        var authToken = new UsernamePasswordAuthenticationToken(claim.get("uuid").toString(), null, roles);
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
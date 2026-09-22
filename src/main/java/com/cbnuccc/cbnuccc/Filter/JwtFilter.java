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
    // 주의: 로그/MDC 관련 부수효과는 여기 두지 않음 - RequestLoggingFilter가 인증 제외 여부와
    // 무관하게 모든 요청의 MDC 생성/정리 및 완료 로그를 전담함
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        HttpMethod requestMethod = HttpMethod.valueOf(request.getMethod());

        return SecurityUtil.EXCLUDE_LIST.stream()
                .anyMatch(exclude -> exclude.method() == requestMethod &&
                        matcher.match(exclude.uriPattern(), requestUri));
    }

    // 필터링하기
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // jwt 토큰을 가져오기 위해 Authorization 헤더 가져오기
        String authString = request.getHeader("Authorization");
        Optional<String> _jwtToken = securityUtil.getAuthorizationToken(authString);
        if (_jwtToken == null) {
            LogUtil.printBasicWarnLog(LogHeader.INVALID_TOKEN);
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
            // 위조/만료 등으로 유효하지 않은 토큰 - 버그는 아니므로 WARN이되, 원인 파악을 위해
            // 예외 상세(만료/서명불일치/형식오류 등)는 그대로 남김
            LogUtil.printBasicWarnLog(LogHeader.INVALID_TOKEN, e);
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

        // 로그인을 위한 최종 설정
        List<SimpleGrantedAuthority> roles = List.of(new SimpleGrantedAuthority("ROLE_" + claim.get("rank")));
        var authToken = new UsernamePasswordAuthenticationToken(claim.get("uuid").toString(), null, roles);
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // MDC 정리는 RequestLoggingFilter(가장 바깥쪽 필터)만 수행함 - 여기서 clear()를 호출하면
        // RequestLoggingFilter가 심어둔 request_id 등이 완료 로그가 찍히기 전에 지워져 버림
        filterChain.doFilter(request, response);
    }
}
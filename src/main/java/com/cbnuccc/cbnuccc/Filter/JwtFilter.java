package com.cbnuccc.cbnuccc.Filter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

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

    private static final AntPathMatcher matcher = new AntPathMatcher();

    // to don't execute twice.
    @Bean
    public FilterRegistrationBean<JwtFilter> disableJwtFilter(JwtFilter filter) {
        FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    // check for not filtering
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        HttpMethod requestMethod = HttpMethod.valueOf(method);

        // for log
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

    // filter
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // get Auth. header to get jwt token.
        String authString = request.getHeader("Authorization");
        Optional<String> _jwtToken = securityUtil.getAuthorizationToken(authString);
        if (_jwtToken == null) {
            response.sendError(
                    StatusCode.INVALID_TOKEN.getResponseStatus().value(),
                    StatusCode.INVALID_TOKEN.getErrorMessage());
            return;
        }
        String jwtToken = _jwtToken.get();

        // extract given token to get cliams.
        Claims claim;
        try {
            claim = securityUtil.extractToken(jwtToken);
        } catch (Exception e) {
            response.sendError(
                    StatusCode.INVALID_TOKEN.getResponseStatus().value(),
                    StatusCode.INVALID_TOKEN.getErrorMessage());
            return;
        }

        // print a entering log
        MDC.put("entered_user_uuid", claim.get("uuid").toString().substring(0, 8));
        LogUtil.printBasicInfoLog(LogHeader.ENTER, (Object[]) null);

        // final setting to login.
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
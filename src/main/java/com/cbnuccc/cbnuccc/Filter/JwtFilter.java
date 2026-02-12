package com.cbnuccc.cbnuccc.Filter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.cbnuccc.cbnuccc.StatusCode;
import com.cbnuccc.cbnuccc.ExcludePath;
import com.cbnuccc.cbnuccc.SecurityUtil;

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

    // list of methods and uris which does not need to get filtered.
    private static final List<ExcludePath> EXCLUDE_LIST = List.of(
            new ExcludePath(HttpMethod.GET, "/user"),
            new ExcludePath(HttpMethod.GET, "/user/*"),
            new ExcludePath(HttpMethod.POST, "/login"),
            new ExcludePath(HttpMethod.GET, "/email"),
            new ExcludePath(HttpMethod.POST, "/email"));

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        HttpMethod requestMethod = HttpMethod.valueOf(request.getMethod());

        return EXCLUDE_LIST.stream()
                .anyMatch(exclude -> exclude.method() == requestMethod &&
                        matcher.match(exclude.uriPattern(), requestUri));
    }

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

        // final setting to login.
        List<SimpleGrantedAuthority> roles = List.of(new SimpleGrantedAuthority("ROLE_" + claim.get("rank")));
        var authToken = new UsernamePasswordAuthenticationToken(claim.get("uuid").toString(), null, roles);
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}
package com.cbnuccc.cbnuccc.Filter;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.cbnuccc.cbnuccc.SecurityUtil;
import com.cbnuccc.cbnuccc.Service.LoginService;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {
    @Autowired
    private LoginService loginService;

    @Autowired
    private SecurityUtil securityUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // get Auth. header to get jwt token.
        String authString = request.getHeader("Authorization");
        Optional<String> _jwtToken = securityUtil.getAuthorizationToken(authString);
        if (_jwtToken == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        String jwtToken = _jwtToken.get();

        // extract given token to get cliams.
        Claims claim;
        try {
            claim = loginService.extractToken(jwtToken);
        } catch (Exception e) {
            System.err.println("Expired token or something went wrong.");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // final setting to login.
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                claim.get("email").toString(), null);
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}
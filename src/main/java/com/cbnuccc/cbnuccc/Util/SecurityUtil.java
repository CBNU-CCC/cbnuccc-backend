package com.cbnuccc.cbnuccc.Util;

import java.util.List;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.Getter;

@Component
@Data
public class SecurityUtil {
    private final String pepper;

    @Getter
    private final SecretKey jwtKey;

    // 주어진 문자열에 chars의 문자가 하나라도 포함되어 있는지 확인하기
    private boolean containsAnyChar(String string, String chars) {
        for (char character : chars.toCharArray()) {
            if (string.indexOf(character) != -1) {
                return true;
            }
        }
        return false;
    }

    // 필터링이 필요 없는 메서드와 uri 목록
    public static final List<ExcludePath> EXCLUDE_LIST = List.of(
            new ExcludePath(HttpMethod.GET, "/email-duplication"),
            new ExcludePath(HttpMethod.POST, "/user"),
            new ExcludePath(HttpMethod.POST, "/login"),
            new ExcludePath(HttpMethod.POST, "/refresh"),
            new ExcludePath(HttpMethod.POST, "/logout"),
            new ExcludePath(HttpMethod.POST, "/verification"),
            new ExcludePath(HttpMethod.POST, "/verification/confirmation"),
            new ExcludePath(HttpMethod.PATCH, "/reset-password"),
            new ExcludePath(HttpMethod.GET, "/swagger-ui.html"),
            new ExcludePath(HttpMethod.GET, "/swagger-ui/**"),
            new ExcludePath(HttpMethod.GET, "/v3/api-docs/**"));

    public SecurityUtil(
            @Value("${pepper}") String pepper,
            @Value("${jwtkey}") String jwtKey) {
        this.pepper = pepper;
        this.jwtKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtKey));
    }

    // pepper가 추가된 비밀번호 반환하기
    public String addPepper(String password) {
        return password + pepper;
    }

    // authString에 "Bearer "가 포함되어 있다면 이를 제거한 토큰을 반환하기
    // 그렇지 않다면 null을 반환하기
    public Optional<String> getAuthorizationToken(String authString) {
        if (authString != null && authString.length() >= 8 && authString.startsWith("Bearer "))
            return Optional.of(authString.substring(7));
        return null;
    }

    // 주어진 jwt 토큰 추출하기
    public Claims extractToken(String token) {
        Claims claims = Jwts.parser().verifyWith(this.jwtKey).build()
                .parseSignedClaims(token).getPayload();
        return claims;
    }

    // 클라이언트 ip 가져오기
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null)
            ip = request.getHeader("Proxy-Client-IP");
        if (ip == null)
            ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip == null)
            ip = request.getHeader("HTTP_CLIENT_IP");
        if (ip == null)
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (ip == null)
            ip = request.getRemoteAddr();

        return ip.split(",")[0];
    }

    // 주어진 비밀번호(평문)가 유효한지 확인하기
    public boolean checkValidPassword(String password) {
        if (password == null)
            return false;

        // 비밀번호 길이는 8~15자여야 함
        if (!(8 <= password.length() && password.length() <= 15))
            return false;

        // 비밀번호는 하나 이상의 특수문자를 포함해야 함
        String specialChars = "`-=~!@#$%^&*()_+{{}|[]\\;':\",./<>?";
        if (!containsAnyChar(password, specialChars))
            return false;

        // 비밀번호는 하나 이상의 숫자를 포함해야 함
        String numbers = "1234567890";
        if (!containsAnyChar(password, numbers))
            return false;

        return true;
    }
}

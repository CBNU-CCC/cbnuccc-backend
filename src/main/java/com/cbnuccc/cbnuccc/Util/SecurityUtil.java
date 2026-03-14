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

    // check contains any character of chars in given string.
    private boolean containsAnyChar(String string, String chars) {
        for (char character : chars.toCharArray()) {
            if (string.indexOf(character) != -1) {
                return true;
            }
        }
        return false;
    }

    // list of methods and uris which does not need to get filtered.
    public static final List<ExcludePath> EXCLUDE_LIST = List.of(
            new ExcludePath(HttpMethod.GET, "/email-duplication"),
            new ExcludePath(HttpMethod.POST, "/user"),
            new ExcludePath(HttpMethod.POST, "/login"),
            new ExcludePath(HttpMethod.POST, "/verification"),
            new ExcludePath(HttpMethod.POST, "/verification/confirmation"));

    public SecurityUtil(
            @Value("${pepper}") String pepper,
            @Value("${jwtkey}") String jwtKey) {
        this.pepper = pepper;
        this.jwtKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtKey));
    }

    // return a password with pepper.
    public String addPepper(String password) {
        return password + pepper;
    }

    // return a token which is authString without "Bearer " if it presents.
    // otherwise, it returns null.
    public Optional<String> getAuthorizationToken(String authString) {
        if (authString != null && authString.length() >= 8 && authString.startsWith("Bearer "))
            return Optional.of(authString.substring(7));
        return null;
    }

    // extract given jwt token.
    public Claims extractToken(String token) {
        Claims claims = Jwts.parser().verifyWith(this.jwtKey).build()
                .parseSignedClaims(token).getPayload();
        return claims;
    }

    // get client ip.
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

    // check if given password(plain password) is vaild or not.
    public boolean checkValidPassword(String password) {
        if (password == null)
            return false;

        // password's length should be 8 to 15.
        if (!(8 <= password.length() && password.length() <= 15))
            return false;

        // password should include one or more special characters.
        String specialChars = "`-=~!@#$%^&*()_+{{}|[]\\;':\",./<>?";
        if (!containsAnyChar(password, specialChars))
            return false;

        // password should include one or more special digits.
        String numbers = "1234567890";
        if (!containsAnyChar(password, numbers))
            return false;

        return true;
    }
}

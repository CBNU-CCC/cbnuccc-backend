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
import lombok.Data;
import lombok.Getter;

@Component
@Data
public class SecurityUtil {
    private final String pepper;

    @Getter
    private final SecretKey jwtKey;

    @Getter
    private final String mailgunKey;

    @Getter
    private final String mailgunDomain;

    // list of methods and uris which does not need to get filtered.
    public static final List<ExcludePath> EXCLUDE_LIST = List.of(
            new ExcludePath(HttpMethod.GET, "/email-duplication"),
            new ExcludePath(HttpMethod.GET, "/user"),
            new ExcludePath(HttpMethod.POST, "/user"),
            new ExcludePath(HttpMethod.GET, "/user/*"),
            new ExcludePath(HttpMethod.POST, "/login"),
            new ExcludePath(HttpMethod.POST, "/verification"),
            new ExcludePath(HttpMethod.POST, "/verification/confirmation"),
            new ExcludePath(HttpMethod.GET, "/profile-image/*"),
            new ExcludePath(HttpMethod.GET, "/prayer"),
            new ExcludePath(HttpMethod.GET, "/prayer/*"),
            new ExcludePath(HttpMethod.GET, "/mission"),
            new ExcludePath(HttpMethod.GET, "/mission/*"));

    public SecurityUtil(
            @Value("${pepper}") String pepper,
            @Value("${jwtkey}") String jwtKey,
            @Value("${mailgun.key}") String mailgunKey,
            @Value("${mailgun.domain}") String mailgunDomain) {
        this.pepper = pepper;
        this.jwtKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtKey));
        this.mailgunKey = mailgunKey;
        this.mailgunDomain = mailgunDomain;
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
}

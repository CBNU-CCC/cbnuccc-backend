package com.cbnuccc.cbnuccc;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class SecurityUtil {
    private final String pepper;
    private final SecretKey jwtKey;

    public SecurityUtil(@Value("${pepper}") String pepper, @Value("${jwtkey}") String jwtKey) {
        this.pepper = pepper;
        this.jwtKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtKey));
    }

    public String addPepper(String password) {
        return password + pepper;
    }

    public SecretKey getJwtKey() {
        return this.jwtKey;
    }
}

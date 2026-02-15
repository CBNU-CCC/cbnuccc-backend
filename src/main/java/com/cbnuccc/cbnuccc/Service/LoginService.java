package com.cbnuccc.cbnuccc.Service;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.cbnuccc.cbnuccc.Model.MyUser;
import com.cbnuccc.cbnuccc.Repository.UserJpaRepository;
import com.cbnuccc.cbnuccc.Util.SecurityUtil;

import io.jsonwebtoken.Jwts;

@Service
public class LoginService {
    private final SecretKey key;
    private final UserJpaRepository userJpaRepository;

    public LoginService(SecurityUtil securityUtil, UserJpaRepository userJpaRepository) {
        this.key = securityUtil.getJwtKey();
        this.userJpaRepository = userJpaRepository;
    }

    // create a jwt token.
    public String createToken(Authentication auth, String email) {
        MyUser user = userJpaRepository.findByEmail(email).get();

        // 1000 ms/s * 60 s/min * 60 min/h * 24 h/d * 7 d = 604800000 ms/d (7 days)
        int expirationMillis = 604800000;
        String jwt = Jwts.builder()
                .claim("uuid", user.getUuid())
                .claim("name", user.getName())
                .claim("sex", user.getSex().toString())
                .claim("rank", user.getRank().toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(key)
                .compact();

        return jwt;
    }
}

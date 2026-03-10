package com.cbnuccc.cbnuccc.Service;

import java.util.Date;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.cbnuccc.cbnuccc.Dto.TokenDto;
import com.cbnuccc.cbnuccc.Model.Login;
import com.cbnuccc.cbnuccc.Model.MyUser;
import com.cbnuccc.cbnuccc.Repository.LoginJpaRepository;
import com.cbnuccc.cbnuccc.Repository.UserJpaRepository;
import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.OffsetDateTimeUtil;
import com.cbnuccc.cbnuccc.Util.SecurityUtil;
import com.cbnuccc.cbnuccc.Util.StatusCode;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginService {
    private final UserJpaRepository userJpaRepository;
    private final SecurityUtil securityUtil;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final LoginJpaRepository loginJpaRepository;

    // create a jwt token.
    private String createToken(Authentication auth, String email, boolean rememberMe) {
        MyUser user = userJpaRepository.findByEmail(email).get();

        // 1000 ms/s * 60 s/min * 60 min/h * 24 h/d * 7 d = 604800000 ms/d (7 days)
        // 1000 ms/s * 60 s/min * 60 min/h * 24 h/d * 1 d = 86400000 ms/d (1 day)
        int expirationMillis = rememberMe ? 604800000 : 86400000;
        String jwt = Jwts.builder()
                .claim("uuid", user.getUuid())
                .claim("name", user.getName())
                .claim("sex", user.getSex().toString())
                .claim("rank", user.getRank().toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(securityUtil.getJwtKey())
                .compact();

        return jwt;
    }

    private boolean checkLoginable(String email, String ip) {
        Optional<Login> _loginRecord = loginJpaRepository.findByEmailAndIp(email, ip);
        if (_loginRecord.isEmpty())
            return true;
        Login loginRecord = _loginRecord.get();

        if (loginRecord.getAttempt() >= 5) {
            if (!loginRecord.getLastLoginAt().isBefore(OffsetDateTimeUtil.getNow().minusMinutes(10))) {
                return false;
            }
        }
        return true;
    }

    public StatusCode recordLoginFailure(String email, String ip) {
        // find a login record by email and ip to update it or create it.
        Optional<Login> _loginRecord = loginJpaRepository.findByEmailAndIp(email, ip);

        Login loginRecord = new Login();
        loginRecord.setAttempt((short) 0);
        loginRecord.setEmail(email);
        loginRecord.setIp(ip);

        // if id exists, then use it to update.
        if (_loginRecord.isPresent())
            loginRecord = _loginRecord.get();

        short attempt = (short) (loginRecord.getAttempt() + 1);
        // if attempt is (over) 5 and current time is not more 10 minutes
        // after the last login time, then make the email and ip locked.
        if (attempt >= 5) {
            if (loginRecord.getLastLoginAt().isBefore(OffsetDateTimeUtil.getNow().minusMinutes(10))) {
                // this function runs when login fails.
                // clear attempts to 1.
                attempt = 1;
            }
        }

        // set last time of logging in now and attempt
        loginRecord.setLastLoginAt(OffsetDateTimeUtil.getNow());
        loginRecord.setAttempt(attempt);

        try {
            if (attempt == 0)
                loginJpaRepository.delete(loginRecord);
            else
                loginJpaRepository.save(loginRecord);
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.LOGIN, LogUtil.makeExceptionKV(e));
            return StatusCode.SOMETHING_WENT_WRONG;
        }

        return StatusCode.NO_ERROR;
    }

    // process login
    public TokenDto login(String email, String password, boolean rememberMe, String ip) {
        // check the email and the ip
        if (!checkLoginable(email, ip))
            return null;

        // find a login record by email and ip to delete it.
        Optional<Login> _loginRecord = loginJpaRepository.findByEmailAndIp(email, ip);
        if (_loginRecord.isPresent()) {
            try {
                loginJpaRepository.delete(_loginRecord.get());
            } catch (Exception e) {
                LogUtil.printBasicWarnLog(LogHeader.LOGIN, LogUtil.makeExceptionKV(e));
                return null;
            }
        }

        // create user's token
        String pepperedPassword = securityUtil.addPepper(password);
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                email, pepperedPassword);
        Authentication auth = null;
        try {
            auth = authenticationManagerBuilder.getObject().authenticate(authToken);
        } catch (AuthenticationException e) {
            // print warn log about failing to login
            LogUtil.printBasicWarnLog(LogHeader.LOGIN, LogUtil.makeEmailKV(email), LogUtil.makeExceptionKV(e));
            return null;
        }
        String token = createToken(auth, email, rememberMe);
        TokenDto tokenDto = new TokenDto(token);

        return tokenDto;
    }
}

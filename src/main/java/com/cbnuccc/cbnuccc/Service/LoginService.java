package com.cbnuccc.cbnuccc.Service;

import java.util.Date;
import java.util.Optional;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@EnableScheduling
public class LoginService {
    private final UserJpaRepository userJpaRepository;
    private final SecurityUtil securityUtil;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final LoginJpaRepository loginJpaRepository;
    private final RefreshTokenService refreshTokenService;

    // 1000 ms/s * 60 s/min * 60 min/h * 1 h = 3600000 ms (1시간)
    private static final long ACCESS_TOKEN_EXPIRATION_MILLIS = 3600000L;

    // access token(jwt) 생성하기
    public String createAccessToken(MyUser user) {
        String jwt = Jwts.builder()
                .claim("uuid", user.getUuid())
                .claim("name", user.getName())
                .claim("sex", user.getSex().toString())
                .claim("rank", user.getRank().toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_MILLIS))
                .signWith(securityUtil.getJwtKey())
                .compact();

        return jwt;
    }

    // 10분마다 불필요한 모든 튜플 삭제하기
    // 1000 ms/s * 60 s/min * 10 min = 600000 (10분)
    @Scheduled(fixedRate = 1000 * 60 * 10)
    @Transactional
    public void deleteAllUselessTupleByLastLoginAt() {
        long countDeletedRows = loginJpaRepository
                .deleteByLastLoginAtBefore(OffsetDateTimeUtil.getNow().minusMinutes(10));

        if (countDeletedRows != 0)
            LogUtil.printBasicInfoLog(LogHeader.SCHEDULED_DELETE_USELESS_LOGIN_RECORD,
                    LogUtil.makeCountKV((int) countDeletedRows));
    }

    // 로그인 가능 여부 확인하기
    public boolean checkLoginable(String email, String ip) {
        Optional<Login> _loginRecord = loginJpaRepository.findByEmailAndIp(email.toLowerCase(), ip);
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
        // 이메일과 ip로 로그인 기록을 찾아 수정하거나 생성하기
        Optional<Login> _loginRecord = loginJpaRepository.findByEmailAndIp(email.toLowerCase(), ip);

        Login loginRecord = new Login();
        loginRecord.setAttempt((short) 0);
        loginRecord.setEmail(email.toLowerCase());
        loginRecord.setIp(ip);

        // id가 존재한다면, 이를 사용하여 수정하기
        if (_loginRecord.isPresent())
            loginRecord = _loginRecord.get();

        short attempt = (short) (loginRecord.getAttempt() + 1);
        // 시도 횟수가 5회 이상이고 현재 시간이 마지막 로그인 시간으로부터
        // 10분 이상 지나지 않았다면, 이메일과 ip를 잠금 처리하기
        if (attempt >= 5) {
            attempt = 5;
            if (loginRecord.getLastLoginAt().isBefore(OffsetDateTimeUtil.getNow().minusMinutes(10))) {
                // 이 함수는 로그인이 실패했을 때 실행됩니다.
                // 시도 횟수를 1로 초기화하기
                attempt = 1;
            }
        }

        // 마지막 로그인 시간을 현재로, 시도 횟수를 설정하기
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

    // 로그인 처리하기
    public TokenDto login(String email, String password, String ip) {
        // 이메일과 ip 확인하기
        if (!checkLoginable(email.toLowerCase(), ip))
            return null;

        // 사용자의 토큰 생성하기
        String pepperedPassword = securityUtil.addPepper(password);
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                email.toLowerCase(), pepperedPassword);
        try {
            authenticationManagerBuilder.getObject().authenticate(authToken);
        } catch (AuthenticationException e) {
            // 로그인 실패에 대한 경고 로그 출력하기
            LogUtil.printBasicWarnLog(LogHeader.LOGIN, LogUtil.makeEmailKV(email), LogUtil.makeExceptionKV(e));
            return null;
        }

        // 이메일과 ip로 로그인 기록을 찾아 삭제하기
        Optional<Login> _loginRecord = loginJpaRepository.findByEmailAndIp(email, ip);
        if (_loginRecord.isPresent()) {
            try {
                loginJpaRepository.delete(_loginRecord.get());
            } catch (Exception e) {
                LogUtil.printBasicWarnLog(LogHeader.LOGIN, LogUtil.makeExceptionKV(e));
                return null;
            }
        }

        MyUser user = userJpaRepository.findByEmail(email.toLowerCase()).get();
        String token = createAccessToken(user);
        String refreshToken = refreshTokenService.issueRefreshToken(user.getId());
        TokenDto tokenDto = new TokenDto(token, refreshToken);

        return tokenDto;
    }
}

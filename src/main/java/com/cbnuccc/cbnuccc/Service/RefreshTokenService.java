package com.cbnuccc.cbnuccc.Service;

import java.security.SecureRandom;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.cbnuccc.cbnuccc.Model.RefreshToken;
import com.cbnuccc.cbnuccc.Repository.RefreshTokenJpaRepository;
import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.OffsetDateTimeUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@EnableScheduling
public class RefreshTokenService {
    private final RefreshTokenJpaRepository refreshTokenJpaRepository;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // 1000 ms/s * 60 s/min * 60 min/h * 24 h/d * 7 d/w * 15 w = 9072000000 ms (15주)
    private static final long REFRESH_TOKEN_EXPIRATION_MILLIS = 9072000000L;

    // 암호학적으로 안전한 opaque 토큰 문자열 생성하기
    private String generateTokenValue() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // 주어진 사용자에 대한 refresh token 발급하기
    public String issueRefreshToken(Long userId) {
        String tokenValue = generateTokenValue();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(tokenValue);
        refreshToken.setExpiresAt(OffsetDateTimeUtil.getNow().plus(REFRESH_TOKEN_EXPIRATION_MILLIS,
                ChronoUnit.MILLIS));
        refreshToken.setCreatedAt(OffsetDateTimeUtil.getNow());

        refreshTokenJpaRepository.save(refreshToken);

        return tokenValue;
    }

    // 주어진 refresh token이 유효한지(존재하고 만료되지 않았는지) 확인하기
    public Optional<RefreshToken> validate(String token) {
        Optional<RefreshToken> _refreshToken = refreshTokenJpaRepository.findByToken(token);
        if (_refreshToken.isEmpty())
            return Optional.empty();

        RefreshToken refreshToken = _refreshToken.get();
        if (refreshToken.getExpiresAt().isBefore(OffsetDateTimeUtil.getNow()))
            return Optional.empty();

        return _refreshToken;
    }

    // 기존 refresh token을 폐기하고 새 refresh token 발급하기
    @Transactional
    public String rotate(String oldToken, Long userId) {
        refreshTokenJpaRepository.deleteByToken(oldToken);
        return issueRefreshToken(userId);
    }

    // 주어진 refresh token 폐기하기
    @Transactional
    public void revoke(String token) {
        refreshTokenJpaRepository.deleteByToken(token);
    }

    // 1시간마다 만료된 모든 refresh token 삭제하기
    // 1000 ms/s * 60 s/min * 60 min/h = 3600000 (1시간)
    @Scheduled(fixedRate = 1000 * 60 * 60)
    @Transactional
    public void deleteAllExpiredRefreshTokens() {
        long countDeletedRows = refreshTokenJpaRepository
                .deleteByExpiresAtBefore(OffsetDateTimeUtil.getNow());

        if (countDeletedRows != 0)
            LogUtil.printBasicInfoLog(LogHeader.SCHEDULED_DELETE_EXPIRED_REFRESH_TOKEN,
                    LogUtil.makeCountKV((int) countDeletedRows));
    }
}

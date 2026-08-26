package com.cbnuccc.cbnuccc.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cbnuccc.cbnuccc.Model.RefreshToken;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, Long> {
    // 토큰 문자열로 refresh token 찾기
    Optional<RefreshToken> findByToken(String token);

    // 토큰 문자열로 refresh token 삭제하기
    void deleteByToken(String token);

    // expires_at이 현재로부터 이전인 모든 튜플 삭제하기
    Long deleteByExpiresAtBefore(OffsetDateTime time);
}

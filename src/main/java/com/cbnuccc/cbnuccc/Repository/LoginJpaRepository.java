package com.cbnuccc.cbnuccc.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cbnuccc.cbnuccc.Model.Login;

public interface LoginJpaRepository extends JpaRepository<Login, Long> {
    // 이메일과 ip로 로그인 데이터 찾기
    Optional<Login> findByEmailAndIp(String email, String ip);

    // last_login_at이 현재로부터 10분 이전인 모든 튜플 삭제하기
    Long deleteByLastLoginAtBefore(OffsetDateTime time);
}

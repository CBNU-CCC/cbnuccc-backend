package com.cbnuccc.cbnuccc.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cbnuccc.cbnuccc.Model.Login;

public interface LoginJpaRepository extends JpaRepository<Login, Long> {
    // find login data by email and ip.
    Optional<Login> findByEmailAndIp(String email, String ip);

    // delete all tuples that the last_login_at is 10 minutes before now.
    Long deleteByLastLoginAtBefore(OffsetDateTime time);
}

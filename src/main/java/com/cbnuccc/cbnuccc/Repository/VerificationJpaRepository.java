package com.cbnuccc.cbnuccc.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cbnuccc.cbnuccc.Model.Verification;

public interface VerificationJpaRepository extends JpaRepository<Verification, Integer> {
    Optional<Verification> findByEmail(String email);

    void deleteByExpireAtBeforeAndIsVerifiedFalse(OffsetDateTime time);
}

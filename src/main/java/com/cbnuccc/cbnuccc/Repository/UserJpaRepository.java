package com.cbnuccc.cbnuccc.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cbnuccc.cbnuccc.Model.MyUser;

public interface UserJpaRepository extends JpaRepository<MyUser, Integer> {
    Optional<MyUser> findByEmail(String email);

    Optional<MyUser> findByUuid(UUID uuid);
}

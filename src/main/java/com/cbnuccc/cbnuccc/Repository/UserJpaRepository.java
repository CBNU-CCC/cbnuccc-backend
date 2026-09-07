package com.cbnuccc.cbnuccc.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.cbnuccc.cbnuccc.Model.MyUser;

public interface UserJpaRepository extends JpaRepository<MyUser, Long> {
    Optional<MyUser> findByEmail(String email);

    Optional<MyUser> findByUuid(UUID uuid);

    @Query("""
            select uuid
            from MyUser
            where affiliatedReviewSoon.id = :id
            """)
    Page<UUID> findAllAffiliatedReviewSoonUsersUuid(long id, Pageable pageable);
}

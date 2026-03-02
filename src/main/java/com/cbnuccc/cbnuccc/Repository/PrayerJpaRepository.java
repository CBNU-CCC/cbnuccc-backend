package com.cbnuccc.cbnuccc.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cbnuccc.cbnuccc.Model.Prayer;

public interface PrayerJpaRepository extends JpaRepository<Prayer, Integer> {
    // get not anonymous prayers.
    Page<Prayer> findAllByAnonymousFalse(Pageable pageable);

    // get all prayers of specific user.
    Page<Prayer> findAllByAuthorUuid(UUID uuid, Pageable pageable);

    // get a specific prayer but not anonymous.
    Optional<Prayer> findByIdAndAnonymousFalse(Integer id);

    // get a specific prayer by id and user.
    Optional<Prayer> findByIdAndAuthorUuid(Integer id, UUID uuid);

    // get author's uuid by prayer's id.
    @Query("""
                select u.uuid
                from Prayer p
                join p.author u
                where p.id = :prayerId
            """)
    Optional<UUID> findAuthorUuidByPrayerId(@Param("prayerId") Integer prayerId);
}

package com.cbnuccc.cbnuccc.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cbnuccc.cbnuccc.Model.Prayer;

public interface PrayerJpaRepository extends JpaRepository<Prayer, Long> {
    // 익명이 아닌 기도 가져오기
    Page<Prayer> findAllByAnonymousFalse(Pageable pageable);

    // 특정 사용자의 모든 기도 가져오기
    Page<Prayer> findAllByAuthorUuid(UUID uuid, Pageable pageable);

    // 익명이 아닌 특정 기도 가져오기
    Optional<Prayer> findByIdAndAnonymousFalse(Integer id);

    // id와 사용자로 특정 기도 가져오기
    Optional<Prayer> findByIdAndAuthorUuid(Integer id, UUID uuid);

    // 기도 id로 작성자의 uuid 가져오기
    @Query("""
                select u.uuid
                from Prayer p
                join p.author u
                where p.id = :prayerId
            """)
    Optional<UUID> findAuthorUuidByPrayerId(@Param("prayerId") Long prayerId);

    // uuid로 기도 개수 가져오기
    int countByAuthorUuid(UUID uuid);

    // 모든 기도 작성자의 uuid 가져오기
    @Query("""
                select distinct u.uuid
                from Prayer p
                join p.author u
            """)
    Page<UUID> findAuthorUuid(Pageable pageable);
}

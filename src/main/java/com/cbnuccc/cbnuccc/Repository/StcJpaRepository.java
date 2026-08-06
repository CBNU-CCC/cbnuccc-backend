package com.cbnuccc.cbnuccc.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cbnuccc.cbnuccc.Model.Stc;

public interface StcJpaRepository extends JpaRepository<Stc, Long> {
    // 주어진 uuid로 모든 STC 정보 찾기
    Page<Stc> findAllByAuthorUuid(UUID uuid, Pageable pageable);

    Optional<Stc> findByIdAndAuthorUuid(Long id, UUID uuid);

    // 작성자의 uuid 가져오기
    @Query("""
                select u.uuid
                from Stc s
                join s.author u
                where s.id = :stcId
            """)
    Optional<UUID> findAuthorUuidByStcId(@Param("stcId") Long stcId);

    // uuid로 STC 정보 개수 가져오기
    int countByAuthorUuid(UUID uuid);

    // 모든 STC 정보의 생성자의 uuid 가져오기
    @Query("""
                select distinct u.uuid
                from Stc s
                join s.author u
            """)
    Page<UUID> findAuthorUuid(Pageable pageable);
}

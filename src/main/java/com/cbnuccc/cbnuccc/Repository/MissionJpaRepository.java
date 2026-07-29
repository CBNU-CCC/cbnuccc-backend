package com.cbnuccc.cbnuccc.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cbnuccc.cbnuccc.Model.Mission;

public interface MissionJpaRepository extends JpaRepository<Mission, Long> {
    // 주어진 uuid로 모든 선교 찾기
    Page<Mission> findAllByAuthorUuid(UUID uuid, Pageable pageable);

    Optional<Mission> findByIdAndAuthorUuid(Long id, UUID uuid);

    // 작성자의 uuid 가져오기
    @Query("""
                select u.uuid
                from Mission m
                join m.author u
                where m.id = :missionId
            """)
    Optional<UUID> findAuthorUuidByMissionId(@Param("missionId") Long missionId);

    // uuid로 선교 개수 가져오기
    int countByAuthorUuid(UUID uuid);

    // 모든 선교의 생성자의 uuid 가져오기
    @Query("""
                select distinct u.uuid
                from Mission m
                join m.author u
            """)
    Page<UUID> findAuthorUuid(Pageable pageable);
}

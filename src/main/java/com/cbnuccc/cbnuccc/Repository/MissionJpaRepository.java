package com.cbnuccc.cbnuccc.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cbnuccc.cbnuccc.Model.Mission;

public interface MissionJpaRepository extends JpaRepository<Mission, Integer> {
    // find all missions by given uuid.
    Page<Mission> findAllByAuthorUuid(UUID uuid, Pageable pageable);

    Optional<Mission> findByIdAndAuthorUuid(int id, UUID uuid);

    // get author's uuid.
    @Query("""
                select u.uuid
                from Mission m
                join m.author u
                where m.id = :missionId
            """)
    Optional<UUID> findAuthorUuidByMissionId(@Param("missionId") Long missionId);

    // get mission count by uuid
    int countByAuthorUuid(UUID uuid);

    // get all user's uuid who created a mission.
    @Query("""
                select distinct u.uuid
                from Mission m
                join m.author u
            """)
    Page<UUID> findAuthorUuid(Pageable pageable);
}

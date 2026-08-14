package com.cbnuccc.cbnuccc.Repository;

import java.time.LocalDate;
import java.util.List;
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

    // 모든 기록된 일자 가져오기
    @Query("""
            select s.recordDate
            from Stc s
            group by s.recordDate
            order by s.recordDate asc
            """)
    List<LocalDate> findAllDates();

    // 항목1에 대해 참여 여부 불러오기
    @Query("""
            select s.topic1
            from Stc s
            where s.author.uuid = :uuid and s.recordDate = :recordDate
            """)
    Optional<Boolean> findTopic1ByAuthorUuidAndRecordDate(@Param("uuid") UUID uuid,
            @Param("recordDate") LocalDate recordDate);

    // 항목2에 대해 참여 여부 불러오기
    @Query("""
            select s.topic2
            from Stc s
            where s.author.uuid = :uuid and s.recordDate = :recordDate
            """)
    Optional<Boolean> findTopic2ByAuthorUuidAndRecordDate(@Param("uuid") UUID uuid,
            @Param("recordDate") LocalDate recordDate);

    // 항목3에 대해 참여 여부 불러오기
    @Query("""
            select s.topic3
            from Stc s
            where s.author.uuid = :uuid and s.recordDate = :recordDate
            """)
    Optional<Boolean> findTopic3ByAuthorUuidAndRecordDate(@Param("uuid") UUID uuid,
            @Param("recordDate") LocalDate recordDate);

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

    // uuid로 topic1이 true인 STC 개수 가져오기
    int countByAuthorUuidAndTopic1True(UUID uuid);

    // uuid로 topic2이 true인 STC 개수 가져오기
    int countByAuthorUuidAndTopic2True(UUID uuid);

    // uuid로 topic3이 true인 STC 개수 가져오기
    int countByAuthorUuidAndTopic3True(UUID uuid);

    // 모든 STC 정보의 생성자의 uuid 가져오기
    @Query("""
                select distinct u.uuid
                from Stc s
                join s.author u
            """)
    Page<UUID> findAuthorUuid(Pageable pageable);
}

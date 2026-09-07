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

        // 주어진 uuid와 일자로 가져오기
        Optional<Stc> findByAuthorUuidAndRecordDate(@Param("uuid") UUID uuid,
                        @Param("recordDate") LocalDate recordDate);

        // uuid로 STC 정보 개수 가져오기
        int countByAuthorUuid(UUID uuid);

        // 모든 STC 정보의 생성자의 uuid 가져오기
        @Query("""
                                select distinct u.uuid
                                from Stc s
                                join s.author u
                        """)
        Page<UUID> findAuthorUuid(Pageable pageable);

        // 주어진 점검순(id) 소속 사용자 중 STC 기록이 있는 uuid 목록
        @Query("""
                                select distinct u.uuid
                                from Stc s
                                join s.author u
                                where u.affiliatedReviewSoon.id = :reviewSoonId
                        """)
        Page<UUID> findAuthorUuidByAffiliatedReviewSoonId(Pageable pageable, Long reviewSoonId);

        // affiliated_review_soon이 NULL인 사용자 중 STC 기록이 있는 uuid 목록 (기타 시트용)
        @Query("""
                                select distinct u.uuid
                                from Stc s
                                join s.author u
                                where u.affiliatedReviewSoon is null
                        """)
        Page<UUID> findAuthorUuidByAffiliatedReviewSoonIsNull(Pageable pageable);
}

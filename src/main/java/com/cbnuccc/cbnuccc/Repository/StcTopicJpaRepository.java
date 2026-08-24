package com.cbnuccc.cbnuccc.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.cbnuccc.cbnuccc.Model.StcTopic;
import com.cbnuccc.cbnuccc.Model.StcTopicId;

public interface StcTopicJpaRepository extends JpaRepository<StcTopic, StcTopicId> {
        // uuid와 항목 번호로 이수 완료된 개수 가져오기
        int countByStcAuthorUuidAndTopicNumberAndCompletionGreaterThanEqual(UUID uuid, Short topicNumber,
                        Short completion);

        // 현재 존재하는 항목 번호 중 최댓값 가져오기
        @Query("select max(t.topicNumber) from StcTopic t")
        Optional<Short> findMaxTopicNumber();
}

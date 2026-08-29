package com.cbnuccc.cbnuccc.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.cbnuccc.cbnuccc.Model.StcTopic;
import com.cbnuccc.cbnuccc.Model.StcTopicId;

public interface StcTopicJpaRepository extends JpaRepository<StcTopic, StcTopicId> {
        // uuid와 항목 번호로 completion 값의 합 가져오기
        @Query("select coalesce(sum(t.completion), 0) from StcTopic t where t.stc.author.uuid = :uuid and t.topicNumber = :topicNumber")
        int sumCompletionByStcAuthorUuidAndTopicNumber(UUID uuid, Short topicNumber);

        // 현재 존재하는 항목 번호 중 최댓값 가져오기
        @Query("select max(t.topicNumber) from StcTopic t")
        Optional<Short> findMaxTopicNumber();
}

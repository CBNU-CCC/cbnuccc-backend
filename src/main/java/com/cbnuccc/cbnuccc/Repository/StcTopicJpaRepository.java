package com.cbnuccc.cbnuccc.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cbnuccc.cbnuccc.Model.StcTopic;
import com.cbnuccc.cbnuccc.Model.StcTopicId;

public interface StcTopicJpaRepository extends JpaRepository<StcTopic, StcTopicId> {
        // uuid와 항목 번호로 이수 완료된 개수 가져오기
        int countByStcAuthorUuidAndTopicNumberAndCompletionTrue(UUID uuid, Short topicNumber);
}

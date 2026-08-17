package com.cbnuccc.cbnuccc.Dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StcDto {
    private Long id;

    // 해당 필드는 DB에 저장되지 않습니다.
    private UUID authorUuid;

    private LocalDate recordDate;

    // 인덱스 + 1이 항목 번호(topic_number)에 대응하는 이수 여부 목록
    private List<Boolean> topics;

    @Nullable
    private String comment;
}

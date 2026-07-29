package com.cbnuccc.cbnuccc.Dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MissionDto {
    private Long id;

    // 해당 필드는 DB에 저장되지 않습니다.
    private UUID authorUuid;

    private OffsetDateTime createdAt;

    private String site;

    private LocalDate startTerm;

    private LocalDate endTerm;

    private String season;

    @Nullable
    private String testimony;

    private Short imageCount;
}

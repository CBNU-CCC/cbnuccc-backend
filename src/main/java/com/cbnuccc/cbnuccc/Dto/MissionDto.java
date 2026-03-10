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

    // this field is not on the DB.
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

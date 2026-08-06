package com.cbnuccc.cbnuccc.Dto;

import java.time.LocalDate;
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

    private Boolean topic1;

    private Boolean topic2;

    private Boolean topic3;

    @Nullable
    private String comment;
}

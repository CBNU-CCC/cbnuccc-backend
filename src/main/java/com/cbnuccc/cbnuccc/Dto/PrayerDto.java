package com.cbnuccc.cbnuccc.Dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PrayerDto {
    private Long id;

    // 해당 필드는 DB에 저장되지 않습니다.
    private UUID authorUuid;

    private OffsetDateTime createdAt;

    private String request;

    private Boolean anonymous;
}

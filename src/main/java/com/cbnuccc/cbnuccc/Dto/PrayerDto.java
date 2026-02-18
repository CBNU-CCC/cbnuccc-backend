package com.cbnuccc.cbnuccc.Dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PrayerDto {
    private Integer id;

    // this field is not on the DB.
    private UUID authorUuid;

    private OffsetDateTime createdAt;

    private String request;

    private Boolean anonymous;
}

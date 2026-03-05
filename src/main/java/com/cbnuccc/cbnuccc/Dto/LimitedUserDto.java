package com.cbnuccc.cbnuccc.Dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LimitedUserDto {
    private UUID uuid;

    private Short rank;

    private String name;

    private Short grade;

    // It's not on the DB.
    private Integer prayerCount = 0;

    public LimitedUserDto() {
    }
}

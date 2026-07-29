package com.cbnuccc.cbnuccc.Dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserDto {
    private UUID uuid;

    private String email;

    private Short rank;

    private Boolean sex;

    private String name;

    private Short grade;

    // 해당 필드는 DB에 저장되지 않습니다.
    private Integer prayerCount = 0;

    // 해당 필드는 DB에 저장되지 않습니다.
    private Integer missionCount = 0;

    public UserDto() {
    }
}

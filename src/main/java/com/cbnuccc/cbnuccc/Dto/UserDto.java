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

    public UserDto() {
    }
}

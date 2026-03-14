package com.cbnuccc.cbnuccc.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResetPasswordDto {
    private String email;

    private String name;

    private String studentId;
}

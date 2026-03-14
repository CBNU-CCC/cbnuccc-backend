package com.cbnuccc.cbnuccc.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OldAndNewPasswordDto {
    private String oldPassword;

    private String newPassword;
}

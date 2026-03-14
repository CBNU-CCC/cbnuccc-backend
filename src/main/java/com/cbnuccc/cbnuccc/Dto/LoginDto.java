package com.cbnuccc.cbnuccc.Dto;

import lombok.Data;

@Data
public class LoginDto {
    private String email;

    private String password;

    private Boolean rememberMe = false;
}

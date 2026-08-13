package com.cbnuccc.cbnuccc.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResetPasswordDto {
    @Schema(description="사용자의 이메일이자 아이디", example = "example123@gmail.com")
    private String email;

    private String name;

    private String studentId;
}

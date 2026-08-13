package com.cbnuccc.cbnuccc.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description="인증토큰 DTO")
@Data
@AllArgsConstructor
public class TokenDto {
    private String token;
}

package com.cbnuccc.cbnuccc.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description="refresh token 관련 DTO")
@Data
public class RefreshTokenDto {
    @Schema(
        description="refresh token",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String refreshToken;
}

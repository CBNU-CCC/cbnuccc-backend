package com.cbnuccc.cbnuccc.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description="로그인 관련 DTO")
@Data
public class LoginDto {
    @Schema(
        description="사용자의 이메일이자 아이디", 
        example = "example123@gmail.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String email;

    @Schema(
        description="사용자의 비밀번호",
        example = "qwerty123!",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String password;
}

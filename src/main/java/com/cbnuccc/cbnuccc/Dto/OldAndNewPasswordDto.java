package com.cbnuccc.cbnuccc.Dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description="비밀번호 변경 DTO")
@Data
@AllArgsConstructor
public class OldAndNewPasswordDto {
    @Schema(
        description="이전 비밀번호",
        example="qwerty123!"
    )
    private String oldPassword;

    @Schema(
        description="새 비밀번호",
        example="qwerty123@"
    )
    private String newPassword;
}

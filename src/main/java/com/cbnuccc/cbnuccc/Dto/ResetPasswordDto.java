package com.cbnuccc.cbnuccc.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResetPasswordDto {
    @Schema(description="사용자의 이메일이자 아이디", example = "example123@gmail.com")
    private String email;

    @Schema(description="사용자의 이름", example = "박순원")
    private String name;

    @Schema(description="사용자의 학번(간사님일 경우 앞에 S기입)", example = "2021041047")
    private String studentId;
}

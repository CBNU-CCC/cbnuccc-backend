package com.cbnuccc.cbnuccc.Dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;


@Schema(description="유저 DTO - 제한적 정보 공개(타인에게 공개)")
@Data
@AllArgsConstructor
public class LimitedUserDto {
    @Schema(
        description="사용자 식별자",
        example = ""
    )
    private UUID uuid;

    @Schema(
        description = "사용자 직분 코드 (0: 순원, 1: 순장, 2: 사역팀 순장, 3: 나사렛 순장, 4: 간사)", 
        example = "3"
    )
    private Short rank;

    @Schema(
        description = "사용자의 이름",
        example = "군바리"
    )
    private String name;

    @Schema(
        description = "사용자의 학년",
        example = "3"
    )
    private Short grade;

    // 해당 필드는 DB에 저장되지 않습니다.
    private Integer prayerCount = 0;

    // 해당 필드는 DB에 저장되지 않습니다.
    private Integer missionCount = 0;

    public LimitedUserDto() {
    }
}

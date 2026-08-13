package com.cbnuccc.cbnuccc.Dto;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description="사용자 정보 DTO")
@Data
@AllArgsConstructor
public class UserDto {
    @Schema(description="사용자 식별자")
    private UUID uuid;

    @Schema(description="사용자의 이메일", example = "example123@gmail.com")
    private String email;

    @Schema(
        description = "사용자 직분 코드 (0: 순원, 1: 순장, 2: 사역팀 순장, 3: 나사렛 순장, 4: 간사)", 
        example = "3"
    )
    private Short rank;

    @Schema(
        description = "사용자 성별 (T: 남성, F: 여성)", 
        example = "true"
    )
    private Boolean sex;

    @Schema(description="사용자의 이름", example = "졸업생")
    private String name;

    @Schema(description="사용자의 학년", example = "0")
    private Short grade;

    // 해당 필드는 DB에 저장되지 않습니다.
    @Schema(description = "기도제목 작성 개수")
    private Integer prayerCount = 0;

    // 해당 필드는 DB에 저장되지 않습니다.
    @Schema(description = "선교 기도편지 개수")
    private Integer missionCount = 0;

    public UserDto() {
    }
}

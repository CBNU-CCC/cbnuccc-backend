package com.cbnuccc.cbnuccc.Dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description="선교 DTO")
@Data
@AllArgsConstructor
public class PrayerDto {
    @Schema(
        description="기도 게시글 식별자 id"
    )
    private Long id;

    // 해당 필드는 DB에 저장되지 않습니다.
    @Schema(
        description="작성자 식별자(UUID)"
    )
    private UUID authorUuid;

    @Schema(
        description="생성 날짜",
        example = "2000/01/01"
    )
    private OffsetDateTime createdAt;

    @Schema(
        description="기도 내용",
        example = "소프트웨어학부의 무궁한 발전과 부흥을 기원하며 이 코드를 작성합니다."
    )
    private String request;

    @Schema(
        description="비공개 여부",
        example = "true",
        defaultValue = "true"
    )
    private Boolean anonymous;
}

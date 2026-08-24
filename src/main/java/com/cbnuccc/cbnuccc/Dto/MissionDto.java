package com.cbnuccc.cbnuccc.Dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class MissionDto {
    @Schema(
        description="선교 게시글 식별자 id"
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

    private String site;

    @Schema(
        description="선교 시작일",
        example = "2000/02/01"
    )
    private LocalDate startTerm;

    @Schema(
        description="선교 종료일",
        example = "2000/02/28"
    )
    private LocalDate endTerm;

    @Schema(
        description="선교 계절",
        example = "2000 겨울"
    )
    private String season;

    @Schema(
        description="간증 후기",
        example ="간증 예시입니다."
    )
    @Nullable
    private String testimony;

    @Schema(
        description="기도편지 이미지 수",
        example = "0"
    )
    private Short imageCount;
}

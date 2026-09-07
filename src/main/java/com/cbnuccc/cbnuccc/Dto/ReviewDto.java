package com.cbnuccc.cbnuccc.Dto;

import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description = "점검 한 마디 작성 DTO")
@Data
@AllArgsConstructor
public class ReviewDto {
    @Schema(description = "점검 한 마디를 받을 사용자의 식별자(UUID)")
    private UUID to;

    @Schema(description = "점검 한 마디를 남길 STC의 기록일", example = "2000-01-01")
    private LocalDate recordDate;

    @Schema(description = "점검 순장이 남기는 한 마디", example = "이번 주도 수고 많았어요!")
    private String message;
}

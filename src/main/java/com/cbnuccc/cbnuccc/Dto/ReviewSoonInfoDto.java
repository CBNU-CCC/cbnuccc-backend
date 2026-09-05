package com.cbnuccc.cbnuccc.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description = "점검순 정보 DTO")
@Data
@AllArgsConstructor
public class ReviewSoonInfoDto {
    @Schema(description = "점검순 식별자", example = "1")
    private Long id;

    @Schema(description = "점검순 이름", example = "1점검순")
    private String name;

    public ReviewSoonInfoDto() {
    }
}

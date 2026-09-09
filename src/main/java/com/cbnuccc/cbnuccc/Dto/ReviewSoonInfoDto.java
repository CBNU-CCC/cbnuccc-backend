package com.cbnuccc.cbnuccc.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;

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

    // primitive boolean으로 선언하면 Lombok이 getter를 isRepresentative()로 만들고
    // Jackson이 이를 "representative"로 오해석해 isRepresentative/representative가 중복 노출됨
    // (getter가 is 프리픽스 없이 getIsRepresentative()로 생성되는) Boolean 래퍼로 선언해 이를 피함
    @JsonProperty("isRepresentative")
    @Schema(description = "요청을 보낸 본인이 이 점검순의 대표(순장)인지 여부. 로그인하지 않은 요청이면 항상 false", example = "false")
    private Boolean isRepresentative;

    public ReviewSoonInfoDto() {
    }
}

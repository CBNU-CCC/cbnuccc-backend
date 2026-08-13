package com.cbnuccc.cbnuccc.Dto;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description="유저 정보 페이지 DTO")
@Data
@AllArgsConstructor
public class PageDto<T> {
    // 데이터
    List<T> data;

    @Schema(
        description="현 페이지의 원소 개수", 
        example="5"
    )
    Integer length;

    @Schema(
        description = "현 페이지 번호",
        example="2"
    ) 
    Integer pageAt;

    @Schema(
        description = "총 페이지 수",
        example="8"
    )
    Integer totalPage;

    @Schema(
        description = "총 원소 개수",
        example = "153"
    ) 
    Long totalElement;
}

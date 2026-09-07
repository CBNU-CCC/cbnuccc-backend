package com.cbnuccc.cbnuccc.Dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description = "STC(점검표) 정보 DTO")
@Data
@AllArgsConstructor
public class StcDto {
    @Schema(description = "STC 식별자 id", example = "1")
    private Long id;

    // 해당 필드는 DB에 저장되지 않습니다.
    @Schema(description = "작성자 식별자(UUID)")
    private UUID authorUuid;

    @Schema(description = "기록일", example = "2000-01-01")
    private LocalDate recordDate;

    // 인덱스 + 1이 항목 번호(topic_number)에 대응하는 이수 여부 목록
    @Schema(description = "항목별 이수 여부 목록 (인덱스 + 1이 항목 번호에 대응)")
    private List<Short> topics;

    // TODO: 기능 개발 완료 후 삭제 요망
    @Schema(description = "코멘트 (기능 개발 완료 후 삭제 예정)")
    @Nullable
    private String comment;

    @Schema(description = "일주일의 삶", example = "이번 주는 은혜로운 한 주였습니다.")
    @Nullable
    private String weeklyLife;

    @Schema(description = "기도제목", example = "가족을 위해 기도해주세요.")
    @Nullable
    private String prayerRequest;

    // 점검 순장님이 입력하는 메세지
    @Schema(description = "점검 순장의 한 마디", example = "이번 주도 수고 많았어요!")
    @Nullable
    private String review;
}

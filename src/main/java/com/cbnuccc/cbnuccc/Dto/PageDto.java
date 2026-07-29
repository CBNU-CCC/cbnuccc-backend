package com.cbnuccc.cbnuccc.Dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageDto<T> {
    // 데이터
    List<T> data;

    // 현 페이지의 원소 개수
    Integer length;

    // 현 페이지 수
    Integer pageAt;

    // 총 페이지 수
    Integer totalPage;

    // 총 원소 개수
    Long totalElement;
}

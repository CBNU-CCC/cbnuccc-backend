package com.cbnuccc.cbnuccc.Dto;

import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReviewDto {
    private UUID to;

    private LocalDate recordDate;

    private String message;
}

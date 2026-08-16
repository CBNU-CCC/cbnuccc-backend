package com.cbnuccc.cbnuccc.Model;

import java.io.Serializable;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StcId implements Serializable {
    private Long author;

    private LocalDate recordDate;
}

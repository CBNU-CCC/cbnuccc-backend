package com.cbnuccc.cbnuccc.Model;

import java.time.LocalDate;

import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@IdClass(StcId.class)
@Table(name = "stc", schema = "public")
public class Stc {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author")
    private MyUser author;

    @Id
    private LocalDate recordDate;

    private Boolean topic1;

    private Boolean topic2;

    private Boolean topic3;

    @Nullable
    private String comment;
}

package com.cbnuccc.cbnuccc.Model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
@Entity
@Table(name = "mission", schema = "public")
public class Mission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(value = AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author")
    private MyUser author;

    private OffsetDateTime createdAt;

    private String site;

    private LocalDate startTerm;

    private LocalDate endTerm;

    private String season;

    @Nullable
    private String testimony;

    private Short imageCount;
}

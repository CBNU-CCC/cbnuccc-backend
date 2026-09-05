package com.cbnuccc.cbnuccc.Model;

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
@Table(name = "review_soon", schema = "public")
public class ReviewSoon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(value = AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id")
    private MyUser id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affiliated_family_soon")
    private ReviewSoonInfo affiliatedFamilySoon;
}

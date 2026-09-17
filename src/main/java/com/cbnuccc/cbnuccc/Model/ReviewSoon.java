package com.cbnuccc.cbnuccc.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "review_soon", schema = "public")
public class ReviewSoon {
    // user.id를 그대로 재사용하는 공유 기본키(review_soon.id -> user.id FK)이므로 자동 생성하지 않음
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affiliated_review_soon")
    private ReviewSoonInfo affiliatedReviewSoon;

    private boolean isRepresentative;
}

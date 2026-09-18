package com.cbnuccc.cbnuccc.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "review_soon_info", schema = "public")
public class ReviewSoonInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // id는 setter를 열어 두어, 사용자 생성/수정 시 MyUser.affiliatedReviewSoon에
    // {"id": N} 형태로 기존 점검순을 참조(FK 연결)할 수 있도록 함
    private Long id;

    private String name;
}

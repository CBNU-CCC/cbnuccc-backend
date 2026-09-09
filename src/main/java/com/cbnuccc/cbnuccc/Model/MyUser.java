package com.cbnuccc.cbnuccc.Model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
@Entity
@Table(name = "user", schema = "public")
public class MyUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(value = AccessLevel.NONE)
    private Long id;

    // UserDto
    private UUID uuid;

    // UserDto
    private String email;

    private String password;

    // UserDto
    private Short rank;

    // UserDto
    private Boolean sex;

    // UserDto
    private String name;

    // UserDto
    private Short grade;

    private String studentId;

    private OffsetDateTime passwordChangedAt;

    @Schema(description = "소속 점검순. id만 채워 기존 점검순을 참조하도록 보낸다. "
            + "생성/수정 시 존재하지 않는 id를 보내면 오류가 발생하며, "
            + "수정 요청에서 필드 자체를 생략하면(null) 기존 소속이 유지되고 빈 객체({})를 보내면 소속이 해제된다.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affiliated_review_soon")
    private ReviewSoonInfo affiliatedReviewSoon;
}

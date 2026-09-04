package com.cbnuccc.cbnuccc.Model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

@Data
@Entity
@Table(name = "stc", schema = "public")
public class Stc {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(value = AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author")
    private MyUser author;

    private LocalDate recordDate;

    // TODO: 기능 개발 완료 후 삭제 요망
    @Nullable
    private String comment;

    @Nullable
    private String weeklyLife;

    @Nullable
    private String prayerRequest;

    // 점검 순장님이 입력하는 메세지
    @Nullable
    private String review;

    // 각 항목별 이수 여부 (stc_topic 테이블에 저장됨). 이 테이블 분리는 사용하는 쪽에서 알 필요 없음
    @OneToMany(mappedBy = "stc", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("topicNumber asc")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<StcTopic> topics = new ArrayList<>();
}

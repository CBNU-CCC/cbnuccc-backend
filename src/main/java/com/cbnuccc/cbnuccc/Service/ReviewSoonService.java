package com.cbnuccc.cbnuccc.Service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.cbnuccc.cbnuccc.Dto.ReviewSoonInfoDto;
import com.cbnuccc.cbnuccc.Model.ReviewSoonInfo;
import com.cbnuccc.cbnuccc.Repository.ReviewSoonInfoJpaRepository;
import com.cbnuccc.cbnuccc.Util.DataWithStatusCode;
import com.cbnuccc.cbnuccc.Util.StatusCode;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewSoonService {
    private final ReviewSoonInfoJpaRepository reviewSoonInfoJpaRepository;

    // ReviewSoonInfo를 ReviewSoonInfoDto로 변환하기
    private ReviewSoonInfoDto reviewSoonInfoToDto(ReviewSoonInfo reviewSoonInfo) {
        return new ReviewSoonInfoDto(reviewSoonInfo.getId(), reviewSoonInfo.getName());
    }

    // 전체 점검순 목록 조회하기
    @Transactional
    public Page<ReviewSoonInfoDto> getAllReviewSoons(Pageable pageable) {
        Page<ReviewSoonInfo> reviewSoons = reviewSoonInfoJpaRepository.findAll(pageable);
        return reviewSoons.map(this::reviewSoonInfoToDto);
    }

    // id로 특정 점검순 조회하기
    @Transactional
    public DataWithStatusCode<ReviewSoonInfoDto> getReviewSoonById(long id) {
        Optional<ReviewSoonInfo> _reviewSoonInfo = reviewSoonInfoJpaRepository.findById(id);
        if (_reviewSoonInfo.isEmpty())
            return new DataWithStatusCode<>(StatusCode.NO_REVIEW_SOON_FOUND, null);

        return new DataWithStatusCode<>(StatusCode.NO_ERROR, reviewSoonInfoToDto(_reviewSoonInfo.get()));
    }
}

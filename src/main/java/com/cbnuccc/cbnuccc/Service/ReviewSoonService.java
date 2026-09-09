package com.cbnuccc.cbnuccc.Service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.cbnuccc.cbnuccc.Dto.ReviewSoonInfoDto;
import com.cbnuccc.cbnuccc.Model.MyUser;
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
    // callerUuid: 요청을 보낸 본인의 uuid (로그인하지 않은 요청이라면 null)
    private ReviewSoonInfoDto reviewSoonInfoToDto(ReviewSoonInfo reviewSoonInfo, UUID callerUuid) {
        MyUser representative = reviewSoonInfo.getRepresentative();
        boolean isRepresentative = callerUuid != null
                && representative != null
                && callerUuid.equals(representative.getUuid());

        return new ReviewSoonInfoDto(reviewSoonInfo.getId(), reviewSoonInfo.getName(), isRepresentative);
    }

    // 전체 점검순 목록 조회하기
    @Transactional
    public Page<ReviewSoonInfoDto> getAllReviewSoons(Pageable pageable, UUID callerUuid) {
        Page<ReviewSoonInfo> reviewSoons = reviewSoonInfoJpaRepository.findAll(pageable);
        return reviewSoons.map(reviewSoonInfo -> reviewSoonInfoToDto(reviewSoonInfo, callerUuid));
    }

    // id로 특정 점검순 조회하기
    @Transactional
    public DataWithStatusCode<ReviewSoonInfoDto> getReviewSoonById(long id, UUID callerUuid) {
        Optional<ReviewSoonInfo> _reviewSoonInfo = reviewSoonInfoJpaRepository.findById(id);
        if (_reviewSoonInfo.isEmpty())
            return new DataWithStatusCode<>(StatusCode.NO_REVIEW_SOON_FOUND, null);

        return new DataWithStatusCode<>(StatusCode.NO_ERROR, reviewSoonInfoToDto(_reviewSoonInfo.get(), callerUuid));
    }
}

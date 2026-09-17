package com.cbnuccc.cbnuccc.Service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.cbnuccc.cbnuccc.Dto.ReviewSoonInfoDto;
import com.cbnuccc.cbnuccc.Model.MyUser;
import com.cbnuccc.cbnuccc.Model.ReviewSoon;
import com.cbnuccc.cbnuccc.Model.ReviewSoonInfo;
import com.cbnuccc.cbnuccc.Repository.ReviewSoonInfoJpaRepository;
import com.cbnuccc.cbnuccc.Repository.ReviewSoonJpaRepository;
import com.cbnuccc.cbnuccc.Repository.UserJpaRepository;
import com.cbnuccc.cbnuccc.Util.DataWithStatusCode;
import com.cbnuccc.cbnuccc.Util.StatusCode;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewSoonService {
    private final ReviewSoonInfoJpaRepository reviewSoonInfoJpaRepository;
    private final ReviewSoonJpaRepository reviewSoonJpaRepository;
    private final UserJpaRepository userJpaRepository;

    // ReviewSoonInfo를 ReviewSoonInfoDto로 변환하기
    // callerUuid: 요청을 보낸 본인의 uuid (로그인하지 않은 요청이라면 null)
    private ReviewSoonInfoDto reviewSoonInfoToDto(ReviewSoonInfo reviewSoonInfo, UUID callerUuid) {
        boolean isRepresentative = isCallerRepresentativeOfGroup(callerUuid, reviewSoonInfo.getId());
        return new ReviewSoonInfoDto(reviewSoonInfo.getId(), reviewSoonInfo.getName(), isRepresentative);
    }

    // 주어진 caller가 주어진 점검순(groupId)의 대표(순장)인지 확인하기
    // 점검순당 대표가 2명 이상 존재할 수 있으므로, "caller 본인의 review_soon 행이 이 그룹을 가리키고
    // is_representative가 true인지"로 판단함
    private boolean isCallerRepresentativeOfGroup(UUID callerUuid, Long groupId) {
        if (callerUuid == null || groupId == null)
            return false;

        Optional<MyUser> _caller = userJpaRepository.findByUuid(callerUuid);
        if (_caller.isEmpty())
            return false;

        Optional<ReviewSoon> _callerReviewSoon = reviewSoonJpaRepository.findById(_caller.get().getId());
        if (_callerReviewSoon.isEmpty())
            return false;

        ReviewSoon callerReviewSoon = _callerReviewSoon.get();
        return callerReviewSoon.isRepresentative()
                && callerReviewSoon.getAffiliatedReviewSoon() != null
                && groupId.equals(callerReviewSoon.getAffiliatedReviewSoon().getId());
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

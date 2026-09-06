package com.cbnuccc.cbnuccc.Controller;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.cbnuccc.cbnuccc.Dto.ReviewDto;
import com.cbnuccc.cbnuccc.Dto.ReviewSoonInfoDto;
import com.cbnuccc.cbnuccc.Dto.StcDto;
import com.cbnuccc.cbnuccc.Dto.UserDto;
import com.cbnuccc.cbnuccc.Service.StcService;
import com.cbnuccc.cbnuccc.Service.UserService;
import com.cbnuccc.cbnuccc.Util.DataWithStatusCode;
import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.PaginationUtil;
import com.cbnuccc.cbnuccc.Util.StatusCode;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class StcController {
    private final UserService userService;
    private final StcService stcService;

    // 내 모든 STC 정보 가져오기
    @GetMapping("/stc")
    public ResponseEntity<?> getMyStcs(Authentication authentication, Pageable pageable) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        Page<StcDto> result = stcService.getAllMyStcs(uuid, pageable);

        LogUtil.printBasicInfoLog(LogHeader.GET_STC,
                LogUtil.makeCountKV(result.getNumberOfElements()),
                LogUtil.makePageNumberKV(pageable),
                LogUtil.makePageSizeKV(pageable));
        return ResponseEntity.ok(PaginationUtil.makePaginationMap(result));
    }

    // 내 특정 STC 정보 하나만 가져오기
    @GetMapping("/stc/{id}")
    public ResponseEntity<?> getMySpecificStc(Authentication authentication, @PathVariable("id") int id) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        DataWithStatusCode<StcDto> result = stcService.getMySpecificStc(id, uuid);
        StatusCode code = result.code();
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.GET_STC, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.GET_STC, LogUtil.makeIdKV(id));
        return ResponseEntity.ok(result.data());
    }

    // STC 정보 생성하기
    @PostMapping("/stc")
    public ResponseEntity<?> createStc(Authentication authentication, @RequestBody StcDto stcDto) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        DataWithStatusCode<StcDto> result = stcService.createStc(stcDto, uuid);
        StatusCode code = result.code();
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.CREATE_STC, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.CREATE_STC, LogUtil.makeIdKV(result.data().getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result.data());
    }

    // 리뷰 작성
    @PostMapping("/stc/review")
    public ResponseEntity<?> createReview(Authentication authentication, @RequestBody ReviewDto reviewDto) {
        // 본인(점검 순장)의 uuid 추출
        UUID uuid = userService.getUuidFromAuth(authentication);

        // 점검 순장의 점검 한 마디를 받을 사용자 추출
        Optional<UserDto> _user = userService.findUserDtoByUuid(reviewDto.getTo());
        if (_user.isEmpty())
            return StatusCode.NO_USER_FOUND.makeErrorResponseEntity();
        UserDto user = _user.get();

        // 주어진 사용자(to)의 점검순의 점검 순장(대표)가 본인인지 확인
        boolean isRepresentative = stcService.isReviewSoonRepresentativeOf(uuid, user.getAffiliatedReviewSoon());
        if (!isRepresentative) {
            // 점검 순장이 아님
            LogUtil.printBasicWarnLog(LogHeader.CREATE_REVIEW_STC,
                    LogUtil.makeStatusCodeMessageKV(StatusCode.INVALID_SOON_REPRESENTATIVE));
            return StatusCode.INVALID_SOON_REPRESENTATIVE.makeErrorResponseEntity();
        }

        // 인증됨 -> 점검 메시지 생성
        StatusCode code = stcService.createMessageToReviewSoonPerson(reviewDto.getTo(), reviewDto.getRecordDate(),
                reviewDto.getMessage());
        if (code.checkIsError())
            LogUtil.printBasicWarnLog(LogHeader.CREATE_REVIEW_STC, LogUtil.makeStatusCodeMessageKV(code));
        else
            LogUtil.printBasicInfoLog(LogHeader.CREATE_REVIEW_STC);
        return code.makeErrorResponseEntity();
    }

    // STC 정보 엑셀로 내려받기
    @GetMapping("/stc/excel")
    public void downloadExcel(HttpServletResponse response) {
        stcService.downloadStc(response);
        LogUtil.printBasicInfoLog(LogHeader.DOWNLOAD_STC);
    }

    // 현재 소속된 점검순 반환하기
    @GetMapping("/stc/review-soon")
    public ResponseEntity<?> getAffiliatedReviewSoon(Authentication authentication) {
        // 현재 사용자 정보 가져오기
        UUID uuid = userService.getUuidFromAuth(authentication);
        DataWithStatusCode<ReviewSoonInfoDto> reviewSoonInfo = stcService.getAffiliatedReviewSoon(uuid);
        if (reviewSoonInfo.code().checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.GET_REVIEW_SOON, LogUtil.makeStatusCodeMessageKV(
                    reviewSoonInfo.code()));
            return reviewSoonInfo.code().makeErrorResponseEntity();
        }

        // 현재 점검순 정보 반환
        return ResponseEntity.ok(reviewSoonInfo.data());
    }

    // 본인이 소속된 점검순에 소속된 모든 사용자 반환하기
    @GetMapping("/stc/review-soon/users")
    public ResponseEntity<?> getAffiliatedReviewSoonUsers(Authentication authentication, Pageable pageable) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        Page<UUID> result = stcService.getAllUsersWhoBelongToAffiliatedReviewSoon(uuid, pageable);
        return ResponseEntity.ok(PaginationUtil.makePaginationMap(result));
    }
}

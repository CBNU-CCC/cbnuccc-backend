package com.cbnuccc.cbnuccc.Controller;

import java.util.Optional;
import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Tag(name = "Stc Controller", description = "STC(점검표) 관련 기능")
@RestController
@RequiredArgsConstructor
public class StcController {
    private final UserService userService;
    private final StcService stcService;

    @Operation(summary = "내 STC(전체) 조회", description = "내가 작성한 모든 STC 정보를 가져온다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "내 STC 목록 조회 성공",
                content = @Content(examples = @ExampleObject(
                    name = "내 STC 목록 응답 예시",
                    value = "{\"data\":[{\"id\":18,\"authorUuid\":\"27ad10b7-3a0f-432a-98e7-6c0290992a4c\",\"recordDate\":\"2026-09-06\",\"topics\":[0,1,1,0,1],\"comment\":null,\"weeklyLife\":\"기쁜 하루다ㅠㅠ\",\"prayerRequest\":\"기쁜 하루가 되길\",\"review\":\"test review message123\"},{\"id\":19,\"authorUuid\":\"27ad10b7-3a0f-432a-98e7-6c0290992a4c\",\"recordDate\":\"2026-09-07\",\"topics\":[1,1,0,1,1],\"comment\":null,\"weeklyLife\":\"테스트용 일주일의 삶입니다.\",\"prayerRequest\":\"테스트용 기도제목입니다.\",\"review\":null}],\"length\":2,\"pageAt\":0,\"totalPage\":1,\"totalElement\":2}"
                ))),
        @ApiResponse(responseCode = "401", description = "인증 실패 (토큰 미제공 또는 만료)", content = @Content)
    })
    @GetMapping("/stc")
    public ResponseEntity<?> getMyStcs(
            @Parameter(hidden = true) Authentication authentication,
            @ParameterObject Pageable pageable) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        Page<StcDto> result = stcService.getAllMyStcs(uuid, pageable);

        LogUtil.printBasicInfoLog(LogHeader.GET_STC,
                LogUtil.makeCountKV(result.getNumberOfElements()),
                LogUtil.makePageNumberKV(pageable),
                LogUtil.makePageSizeKV(pageable));
        return ResponseEntity.ok(PaginationUtil.makePaginationMap(result));
    }

    @Operation(summary = "내 STC(단일) 조회", description = "ID에 해당하는 내 STC 정보 하나만 가져온다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "내 STC 조회 성공",
                content = @Content(examples = @ExampleObject(
                    name = "내 STC 단일 응답 예시",
                    value = "{\"id\":18,\"authorUuid\":\"27ad10b7-3a0f-432a-98e7-6c0290992a4c\",\"recordDate\":\"2026-09-06\",\"topics\":[0,1,1,0,1],\"comment\":null,\"weeklyLife\":\"기쁜 하루다ㅠㅠ\",\"prayerRequest\":\"기쁜 하루가 되길\",\"review\":\"test review message123\"}"
                ))),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(responseCode = "404", description = "해당 ID의 STC를 찾을 수 없거나 접근 권한 없음",
                content = @Content(examples = @ExampleObject(
                    name = "STC 없음 응답 예시",
                    value = "{\"errorCode\":21,\"message\":\"주어진 STC 정보가 존재하지 않습니다.\"}"
                )))
    })
    @GetMapping("/stc/{id}")
    public ResponseEntity<?> getMySpecificStc(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "조회할 STC ID", example = "1") @PathVariable("id") int id) {
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

    @Operation(summary = "STC 생성", description = "새로운 STC(점검표) 정보를 작성한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "STC 생성 성공",
                content = @Content(examples = @ExampleObject(
                    name = "STC 생성 응답 예시",
                    value = "{\"id\":19,\"authorUuid\":\"27ad10b7-3a0f-432a-98e7-6c0290992a4c\",\"recordDate\":\"2026-09-07\",\"topics\":[1,1,0,1,1],\"comment\":null,\"weeklyLife\":\"테스트용 일주일의 삶입니다.\",\"prayerRequest\":\"테스트용 기도제목입니다.\",\"review\":null}"
                ))),
        @ApiResponse(responseCode = "400", description = "입력값 검증 실패", content = @Content),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @PostMapping("/stc")
    public ResponseEntity<?> createStc(
            @Parameter(hidden = true) Authentication authentication,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "생성할 STC 정보",
                required = true,
                content = @Content(examples = @ExampleObject(
                    name = "STC 생성 요청 예시",
                    value = "{\"id\":null,\"authorUuid\":null,\"recordDate\":\"2026-09-07\",\"topics\":[1,1,0,1,1],\"comment\":null,\"weeklyLife\":\"테스트용 일주일의 삶입니다.\",\"prayerRequest\":\"테스트용 기도제목입니다.\",\"review\":null}"
                )))
            @RequestBody StcDto stcDto) {
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

    @Operation(summary = "점검 한 마디 작성", description = "본인이 점검 순장/대표로 있는 점검순 소속 사용자의 STC에 점검 한 마디를 작성한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "점검 한 마디 작성 성공",
                content = @Content(examples = @ExampleObject(
                    name = "점검 한 마디 작성 성공 응답 예시",
                    value = "{\"errorCode\":-1,\"message\":\"오류가 없습니다.\"}"
                ))),
        @ApiResponse(responseCode = "401", description = "인증 실패 또는 점검 순장/대표가 아님",
                content = @Content(examples = @ExampleObject(
                    name = "점검 순장/대표 아님 응답 예시",
                    value = "{\"errorCode\":24,\"message\":\"주어진 사용자가 소속된 점검순의 점검 순장/대표가 본인이 아닙니다.\"}"
                ))),
        @ApiResponse(responseCode = "404", description = "대상 사용자 또는 해당 기록일의 STC를 찾을 수 없음",
                content = @Content(examples = @ExampleObject(
                    name = "대상 사용자 없음 응답 예시",
                    value = "{\"errorCode\":2,\"message\":\"구하고자 하는 사용자가 존재하지 않습니다.\"}"
                )))
    })
    @PostMapping("/stc/review")
    public ResponseEntity<?> createReview(
            @Parameter(hidden = true) Authentication authentication,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "작성할 점검 한 마디 정보",
                required = true,
                content = @Content(examples = @ExampleObject(
                    name = "점검 한 마디 작성 요청 예시",
                    value = "{\"to\":\"27ad10b7-3a0f-432a-98e7-6c0290992a4c\",\"recordDate\":\"2026-09-06\",\"message\":\"수고했어요!\"}"
                )))
            @RequestBody ReviewDto reviewDto) {
        // 본인(점검 순장)의 uuid 추출
        UUID uuid = userService.getUuidFromAuth(authentication);

        // 점검 순장의 점검 한 마디를 받을 사용자 추출
        Optional<UserDto> _user = userService.findUserDtoByUuid(reviewDto.getTo(), uuid);
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

    @Operation(summary = "STC 엑셀 다운로드", description = "모든 사용자의 STC 정보를 점검순별 시트로 나눈 엑셀 파일(.xlsx)로 내려받는다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "엑셀 다운로드 성공",
                content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    })
    @GetMapping("/stc/excel")
    public void downloadExcel(@Parameter(hidden = true) HttpServletResponse response) {
        stcService.downloadStc(response);
        LogUtil.printBasicInfoLog(LogHeader.DOWNLOAD_STC);
    }

    @Operation(summary = "소속 점검순 조회", description = "현재 사용자가 소속된 점검순 정보를 반환한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "소속 점검순 조회 성공",
                content = @Content(schema = @Schema(implementation = ReviewSoonInfoDto.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없거나 소속된 점검순이 없음",
                content = @Content(examples = @ExampleObject(
                    name = "소속 점검순 없음 응답 예시",
                    value = "{\"errorCode\":25,\"message\":\"주어진 사용자가 소속된 점검순이 존재하지 않습니다.\"}"
                )))
    })
    @GetMapping("/stc/review-soon")
    public ResponseEntity<?> getAffiliatedReviewSoon(@Parameter(hidden = true) Authentication authentication) {
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

    @Operation(summary = "소속 점검순 사용자 목록 조회", description = "본인이 소속된 점검순에 소속된 모든 사용자의 uuid를 반환한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "소속 점검순 사용자 목록 조회 성공",
                content = @Content(examples = @ExampleObject(
                    name = "소속 점검순 사용자 목록 응답 예시",
                    value = "{\"data\":[],\"length\":0,\"pageAt\":0,\"totalPage\":1,\"totalElement\":0}"
                ))),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @GetMapping("/stc/review-soon/users")
    public ResponseEntity<?> getAffiliatedReviewSoonUsers(
            @Parameter(hidden = true) Authentication authentication,
            @ParameterObject Pageable pageable) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        Page<UUID> result = stcService.getAllUsersWhoBelongToAffiliatedReviewSoon(uuid, pageable);
        return ResponseEntity.ok(PaginationUtil.makePaginationMap(result));
    }
}

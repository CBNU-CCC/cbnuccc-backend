package com.cbnuccc.cbnuccc.Controller;

import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.cbnuccc.cbnuccc.Dto.ReviewSoonInfoDto;
import com.cbnuccc.cbnuccc.Service.ReviewSoonService;
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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Review Soon Controller", description = "점검순 관련 기능")
@RestController
@RequiredArgsConstructor
public class ReviewSoonController {
    private final ReviewSoonService reviewSoonService;
    private final UserService userService;

    @Operation(summary = "전체 점검순 목록 조회", description = "존재하는 모든 점검순 정보를 가져온다. "
            + "각 점검순의 isRepresentative는 본인이 해당 점검순의 대표인지 여부를 나타낸다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "전체 점검순 목록 조회 성공",
                content = @Content(examples = @ExampleObject(
                    name = "전체 점검순 목록 응답 예시",
                    value = "{\"data\":[{\"id\":1,\"name\":\"1점검순\",\"isRepresentative\":true},{\"id\":2,\"name\":\"2점검순\",\"isRepresentative\":false}],\"length\":2,\"pageAt\":0,\"totalPage\":1,\"totalElement\":2}"
                ))),
        @ApiResponse(responseCode = "401", description = "인증 실패 (토큰 미제공 또는 만료)", content = @Content)
    })
    @GetMapping("/review-soon")
    public ResponseEntity<?> getAllReviewSoons(
            @Parameter(hidden = true) Authentication authentication,
            @ParameterObject Pageable pageable) {
        UUID callerUuid = userService.getUuidFromAuth(authentication);
        Page<ReviewSoonInfoDto> result = reviewSoonService.getAllReviewSoons(pageable, callerUuid);

        LogUtil.printBasicInfoLog(LogHeader.GET_REVIEW_SOON_LIST,
                LogUtil.makeCountKV(result.getNumberOfElements()),
                LogUtil.makePageNumberKV(pageable),
                LogUtil.makePageSizeKV(pageable));
        return ResponseEntity.ok(PaginationUtil.makePaginationMap(result));
    }

    @Operation(summary = "특정 점검순 조회", description = "ID에 해당하는 점검순 정보를 가져온다. "
            + "isRepresentative는 본인이 해당 점검순의 대표인지 여부를 나타낸다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "점검순 조회 성공",
                content = @Content(examples = @ExampleObject(
                    name = "점검순 조회 응답 예시",
                    value = "{\"id\":1,\"name\":\"1점검순\",\"isRepresentative\":true}"
                ))),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(responseCode = "404", description = "해당 ID의 점검순을 찾을 수 없음",
                content = @Content(examples = @ExampleObject(
                    name = "점검순 없음 응답 예시",
                    value = "{\"errorCode\":25,\"message\":\"주어진 사용자가 소속된 점검순이 존재하지 않습니다.\"}"
                )))
    })
    @GetMapping("/review-soon/{id}")
    public ResponseEntity<?> getReviewSoonById(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "조회할 점검순 ID", example = "1") @PathVariable("id") long id) {
        UUID callerUuid = userService.getUuidFromAuth(authentication);
        DataWithStatusCode<ReviewSoonInfoDto> result = reviewSoonService.getReviewSoonById(id, callerUuid);
        StatusCode code = result.code();
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.GET_REVIEW_SOON_LIST, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.GET_REVIEW_SOON_LIST, LogUtil.makeIdKV(id));
        return ResponseEntity.ok(result.data());
    }
}

package com.cbnuccc.cbnuccc.Controller;

import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.cbnuccc.cbnuccc.Dto.PrayerDto;
import com.cbnuccc.cbnuccc.Service.PrayerService;
import com.cbnuccc.cbnuccc.Service.UserService;
import com.cbnuccc.cbnuccc.Util.DataWithStatusCode;
import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.PaginationUtil;
import com.cbnuccc.cbnuccc.Util.StatusCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name="Prayer Controller", description="기도 나무 관련 기능")
@RestController
@RequiredArgsConstructor
public class PrayerController {
    private final UserService userService;
    private final PrayerService prayerService;

    @Operation(summary="모든 공개된 기도 가져오기", description = "기도 나무에 등록된 모든 공개된 기도들을 가져온다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "공개 기도 목록 조회 성공")
    })
    @GetMapping("/prayer")
    public ResponseEntity<?> getPrayers(@ParameterObject Pageable pageable) {
        Page<PrayerDto> result = prayerService.getAllNotAnonymousPrayers(pageable);

        LogUtil.printBasicInfoLog(LogHeader.GET_PRAYER,
                LogUtil.makeCountKV(result.getNumberOfElements()),
                LogUtil.makePageNumberKV(pageable),
                LogUtil.makePageSizeKV(pageable));
        return ResponseEntity.ok(PaginationUtil.makePaginationMap(result));
    }

    @Operation(summary="기도(단일) 조회", description = "특정 기도 하나만 가져오기")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "기도 조회 성공"),
        @ApiResponse(responseCode = "404", description = "해당 ID의 공개 기도를 찾을 수 없음", content = @Content)
    })
    @GetMapping("/prayer/{id}")
    public ResponseEntity<?> getPrayersById(@Parameter(description = "조회할 기도 ID", example = "1") @PathVariable("id") int id) {
        DataWithStatusCode<PrayerDto> result = prayerService.getNotAnonymousSpecificPrayer(id);
        StatusCode code = result.code();
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.GET_PRAYER, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.GET_PRAYER, LogUtil.makeIdKV(id));
        return ResponseEntity.ok(result.data());
    }

    @Operation(summary="나의 기도(전체) 조회", description = "내 모든 기도 가져오기")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "내 기도 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패 (토큰 미제공 또는 만료)", content = @Content)
    })
    @GetMapping("/my-prayer")
    public ResponseEntity<?> getMyPrayers(
            @Parameter(hidden = true) Authentication authentication,
            @ParameterObject Pageable pageable) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        Page<PrayerDto> result = prayerService.getAllPrayersByUuid(uuid, pageable);

        LogUtil.printBasicInfoLog(LogHeader.GET_PRAYER,
                LogUtil.makeCountKV(result.getNumberOfElements()),
                LogUtil.makePageNumberKV(pageable),
                LogUtil.makePageSizeKV(pageable));
        return ResponseEntity.ok(PaginationUtil.makePaginationMap(result));
    }

    @Operation(summary="나의 기도(단일) 조회", description = "내 특정 기도 하나만 가져오기")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "내 기도 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(responseCode = "404", description = "기도를 찾을 수 없거나 접근 권한 없음", content = @Content)
    })
    @GetMapping("/my-prayer/{id}")
    public ResponseEntity<?> getMyPrayerById(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "조회할 기도 ID", example = "1") @PathVariable("id") int id) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        DataWithStatusCode<PrayerDto> result = prayerService.getPrayerById(id, uuid);
        StatusCode code = result.code();
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.GET_PRAYER, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.GET_PRAYER, LogUtil.makeIdKV(id));
        return ResponseEntity.ok(result.data());
    }

    @Operation(summary="기도 생성", description = "기도를 생성한다")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "기도 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 입력값", content = @Content),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @PostMapping("/prayer")
    public ResponseEntity<?> createPrayer(
            @Parameter(hidden = true) Authentication authentication,
            @RequestBody PrayerDto prayerDto) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        DataWithStatusCode<PrayerDto> result = prayerService.createPrayer(prayerDto, uuid);
        StatusCode code = result.code();
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.CREATE_PRAYER, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity(); //맞는진 몰루
        }

        LogUtil.printBasicInfoLog(LogHeader.CREATE_PRAYER, LogUtil.makeIdKV(result.data().getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result.data());
    }

    @Operation(summary="기도 수정", description = "기도를 수정한다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "기도 수정 성공"),
        @ApiResponse(responseCode = "403", description = "수정 권한 없음", content = @Content),
        @ApiResponse(responseCode = "404", description = "기도 게시글 없음", content = @Content)
    })
    @PatchMapping("/prayer/{id}")
    public ResponseEntity<?> updatePrayer(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "수정할 기도 ID", example = "1") @PathVariable("id") int id,
            @RequestBody PrayerDto prayerDto) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        StatusCode code = prayerService.updatePrayer(id, uuid, prayerDto);
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.UPDATE_PRAYER, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.UPDATE_PRAYER, LogUtil.makeIdKV(id));
        return getMyPrayerById(authentication, id);
    }

    @Operation(summary="기도 삭제", description = "기도를 삭제한다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "기도 삭제 성공"),
        @ApiResponse(responseCode = "403", description = "삭제 권한 없음", content = @Content),
        @ApiResponse(responseCode = "404", description = "기도 게시글 없음", content = @Content)
    })
    @DeleteMapping("/prayer/{id}")
    public ResponseEntity<?> deletePrayer(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "삭제할 기도 ID", example = "1") @PathVariable("id") int id) {
        // 작성자 uuid 가져오기
        UUID uuid = userService.getUuidFromAuth(authentication);

        // 삭제될 데이터 가져오기
        DataWithStatusCode<PrayerDto> _deletedPrayer = prayerService.getPrayerById(id, uuid);
        StatusCode code = _deletedPrayer.code();
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.DELETE_PRAYER, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }
        PrayerDto deletedPrayer = _deletedPrayer.data();

        // 삭제하기
        StatusCode result = prayerService.deletePrayer(id, uuid);
        if (result.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.DELETE_PRAYER, LogUtil.makeStatusCodeMessageKV(code));
            return result.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.DELETE_PRAYER, LogUtil.makeIdKV(id));
        return ResponseEntity.ok(deletedPrayer);
    }

    @Operation(summary="기도 작성자 조회", description = "모든 기도 작성자를 가져온다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "작성자 UUID 목록 조회 성공")
    })
    @GetMapping("/prayer/author")
    public ResponseEntity<?> getAllAuthorUuid(@ParameterObject Pageable pageable) {
        Page<UUID> uuids = prayerService.getAllAuthorUuid(pageable);
        LogUtil.printBasicInfoLog(LogHeader.GET_PRAYER_AUTHOR,
                LogUtil.makeCountKV(uuids.getNumber()),
                LogUtil.makePageNumberKV(pageable),
                LogUtil.makePageSizeKV(pageable));
        return ResponseEntity.ok(PaginationUtil.makePaginationMap(uuids));
    }
}

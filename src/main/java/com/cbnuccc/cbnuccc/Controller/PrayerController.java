package com.cbnuccc.cbnuccc.Controller;

import java.util.UUID;

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

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PrayerController {
    private final UserService userService;
    private final PrayerService prayerService;

    // 모든 공개된 기도 가져오기
    @GetMapping("/prayer")
    public ResponseEntity<?> getPrayers(Pageable pageable) {
        Page<PrayerDto> result = prayerService.getAllNotAnonymousPrayers(pageable);

        LogUtil.printBasicInfoLog(LogHeader.GET_PRAYER,
                LogUtil.makeCountKV(result.getNumberOfElements()),
                LogUtil.makePageNumberKV(pageable),
                LogUtil.makePageSizeKV(pageable));
        return ResponseEntity.ok(PaginationUtil.makePaginationMap(result));
    }

    // 특정 기도 하나만 가져오기
    @GetMapping("/prayer/{id}")
    public ResponseEntity<?> getPrayersById(@PathVariable("id") int id) {
        DataWithStatusCode<PrayerDto> result = prayerService.getNotAnonymousSpecificPrayer(id);
        StatusCode code = result.code();
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.GET_PRAYER, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.GET_PRAYER, LogUtil.makeIdKV(id));
        return ResponseEntity.ok(result.data());
    }

    // 내 모든 기도 가져오기
    @GetMapping("/my-prayer")
    public ResponseEntity<?> getMyPrayers(Authentication authentication, Pageable pageable) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        Page<PrayerDto> result = prayerService.getAllPrayersByUuid(uuid, pageable);

        LogUtil.printBasicInfoLog(LogHeader.GET_PRAYER,
                LogUtil.makeCountKV(result.getNumberOfElements()),
                LogUtil.makePageNumberKV(pageable),
                LogUtil.makePageSizeKV(pageable));
        return ResponseEntity.ok(PaginationUtil.makePaginationMap(result));
    }

    // 내 특정 기도 하나만 가져오기
    @GetMapping("/my-prayer/{id}")
    public ResponseEntity<?> getMyPrayerById(Authentication authentication, @PathVariable("id") int id) {
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

    // 기도 생성하기
    @PostMapping("/prayer")
    public ResponseEntity<?> createPrayer(Authentication authentication, @RequestBody PrayerDto prayerDto) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        DataWithStatusCode<PrayerDto> result = prayerService.createPrayer(prayerDto, uuid);
        StatusCode code = result.code();
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.CREATE_PRAYER, LogUtil.makeStatusCodeMessageKV(code));
            code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.CREATE_PRAYER, LogUtil.makeIdKV(result.data().getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result.data());
    }

    // 기도 수정하기
    @PatchMapping("/prayer/{id}")
    public ResponseEntity<?> updatePrayer(
            Authentication authentication,
            @PathVariable("id") int id,
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

    // 기도 삭제하기
    @DeleteMapping("/prayer/{id}")
    public ResponseEntity<?> deletePrayer(
            Authentication authentication,
            @PathVariable("id") int id) {
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

    // 모든 기도 작성자 가져오기
    @GetMapping("/prayer/author")
    public ResponseEntity<?> getAllAuthorUuid(Pageable pageable) {
        Page<UUID> uuids = prayerService.getAllAuthorUuid(pageable);
        LogUtil.printBasicInfoLog(LogHeader.GET_PRAYER_AUTHOR,
                LogUtil.makeCountKV(uuids.getNumber()),
                LogUtil.makePageNumberKV(pageable),
                LogUtil.makePageSizeKV(pageable));
        return ResponseEntity.ok(PaginationUtil.makePaginationMap(uuids));
    }
}

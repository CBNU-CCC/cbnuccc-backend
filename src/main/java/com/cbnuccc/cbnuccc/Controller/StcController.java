package com.cbnuccc.cbnuccc.Controller;

import java.time.LocalDate;
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

import com.cbnuccc.cbnuccc.Dto.StcDto;
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
    @GetMapping("/stc/{recordDate}")
    public ResponseEntity<?> getMySpecificStc(Authentication authentication,
            @PathVariable("recordDate") LocalDate recordDate) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        DataWithStatusCode<StcDto> result = stcService.getMySpecificStc(recordDate, uuid);
        StatusCode code = result.code();
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.GET_STC, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.GET_STC, LogUtil.makeRecordDateKV(recordDate));
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

        LogUtil.printBasicInfoLog(LogHeader.CREATE_STC, LogUtil.makeRecordDateKV(result.data().getRecordDate()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result.data());
    }

    // STC 정보 엑셀로 내려받기
    @GetMapping("/stc/excel")
    public void downloadExcel(HttpServletResponse response) {
        stcService.downloadStc(response);
    }
}

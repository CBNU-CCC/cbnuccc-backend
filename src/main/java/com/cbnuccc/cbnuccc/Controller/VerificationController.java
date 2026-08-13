package com.cbnuccc.cbnuccc.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.cbnuccc.cbnuccc.Service.VerificationService;
import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.StatusCode;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;

@Tag(name="인증 Controller", description="인증 기능")
@RestController
public class VerificationController {
    @Autowired
    VerificationService verificationService;

    @Operation(summary = "인증 이메일 전송", description="이메일로 6자리 인증용 코드 전송하기")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "인증 이메일 발송 성공"
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "필수 인자 누락 (email 키 없음) 또는 이메일 전송/DB 저장 실패",
            content = @Content
        )
    })
    @PostMapping("/verification")
    public ResponseEntity<?> sendEmailToVerify(@RequestBody Map<String, String> body) {
        // body로 전달된 인자 확인하기
        if (!body.containsKey("email")) {
            LogUtil.printBasicWarnLog(LogHeader.SEND_REGISTRATION_EMAIL,
                    LogUtil.makeStatusCodeMessageKV(StatusCode.NO_ENOUGH_ARGS));
            return StatusCode.NO_ENOUGH_ARGS.makeErrorResponseEntity();
        }

        String email = body.get("email").toLowerCase();
        final String code = verificationService.makeCode();

        // 코드와 함께 전송하기
        StatusCode errCode = verificationService.sendEmailCode(email, code);
        if (errCode.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.SEND_REGISTRATION_EMAIL, LogUtil.makeStatusCodeMessageKV(errCode));
            return errCode.makeErrorResponseEntity();
        }

        // 해당 데이터를 DB에 저장하기
        errCode = verificationService.saveEmailVerification(email, code);
        if (errCode.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.SEND_REGISTRATION_EMAIL, LogUtil.makeStatusCodeMessageKV(errCode));
            return errCode.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.SEND_REGISTRATION_EMAIL, (Object[]) null);
        return StatusCode.NO_ERROR.makeErrorResponseEntity();
    }
    @Operation(
        summary = "인증 코드 확인", 
        description = "사용자가 입력한 인증 코드가 이메일로 발송된 코드와 일치하는지 검증합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "인증 성공"
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "필수 인자 누락 (email 또는 code 없음) 또는 인증 코드 불일치/만료",
            content = @Content
        )
    })
    @PostMapping("/verification/confirmation")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> body) {
        // body로 전달된 인자 확인하기
        if (!(body.containsKey("email") && body.containsKey("code"))) {
            LogUtil.printBasicWarnLog(LogHeader.CONFIRM_REGISTRATION_CODE,
                    LogUtil.makeStatusCodeMessageKV(StatusCode.NO_ENOUGH_ARGS));
            return StatusCode.NO_ENOUGH_ARGS.makeErrorResponseEntity();
        }

        String email = body.get("email").toLowerCase();
        String code = body.get("code");

        // 로그 출력 및 반환
        StatusCode errCode = verificationService.verifyCode(email, code);
        if (errCode.checkIsError())
            LogUtil.printBasicWarnLog(LogHeader.CONFIRM_REGISTRATION_CODE, LogUtil.makeStatusCodeMessageKV(errCode));
        else
            LogUtil.printBasicInfoLog(LogHeader.CONFIRM_REGISTRATION_CODE, (Object[]) null);
        return errCode.makeErrorResponseEntity();
    }
}

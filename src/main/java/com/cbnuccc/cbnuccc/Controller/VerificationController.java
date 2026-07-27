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

@RestController
public class VerificationController {
    @Autowired
    VerificationService verificationService;

    // 이메일 인증용 이메일 전송하기
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

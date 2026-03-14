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

    @PostMapping("/verification")
    public ResponseEntity<?> sendEmailToVerify(@RequestBody Map<String, String> body) {
        // check args
        if (!body.containsKey("email")) {
            LogUtil.printBasicWarnLog(LogHeader.SEND_REGISTRATION_EMAIL,
                    LogUtil.makeStatusCodeMessageKV(StatusCode.NO_ENOUGH_ARGS));
            return StatusCode.NO_ENOUGH_ARGS.makeErrorResponseEntity();
        }

        String email = body.get("email");
        final String code = verificationService.makeCode();

        // send mail with code
        StatusCode errCode = verificationService.sendEmailCode(email, code);
        if (errCode.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.SEND_REGISTRATION_EMAIL, LogUtil.makeStatusCodeMessageKV(errCode));
            return errCode.makeErrorResponseEntity();
        }

        // save data to verification table
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
        // check args
        if (!(body.containsKey("email") && body.containsKey("code"))) {
            LogUtil.printBasicWarnLog(LogHeader.CONFIRM_REGISTRATION_CODE,
                    LogUtil.makeStatusCodeMessageKV(StatusCode.NO_ENOUGH_ARGS));
            return StatusCode.NO_ENOUGH_ARGS.makeErrorResponseEntity();
        }

        String email = body.get("email");
        String code = body.get("code");

        // print log and return
        StatusCode errCode = verificationService.verifyCode(email, code);
        if (errCode.checkIsError())
            LogUtil.printBasicWarnLog(LogHeader.CONFIRM_REGISTRATION_CODE, LogUtil.makeStatusCodeMessageKV(errCode));
        else
            LogUtil.printBasicInfoLog(LogHeader.CONFIRM_REGISTRATION_CODE, (Object[]) null);
        return errCode.makeErrorResponseEntity();
    }
}

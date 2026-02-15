package com.cbnuccc.cbnuccc.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.cbnuccc.cbnuccc.Service.VerificationService;
import com.cbnuccc.cbnuccc.Util.StatusCode;

@RestController
public class VerificationController {
    @Autowired
    VerificationService verificationService;

    @PostMapping("/verification")
    public ResponseEntity<?> sendEmailToVerify(@RequestBody Map<String, String> body) {
        if (!body.containsKey("email"))
            return StatusCode.NO_ENOUGH_ARGS.makeErrorResponseEntity();

        String email = body.get("email");
        final String code = verificationService.makeCode();

        StatusCode err = verificationService.saveEmailVerification(email, code);
        if (err.checkIsError())
            return err.makeErrorResponseEntity();

        err = verificationService.sendMailCode(email, code);
        if (err.checkIsError())
            return err.makeErrorResponseEntity();

        return StatusCode.NO_ERROR.makeErrorResponseEntity();
    }

    @PostMapping("/verification/confirmation")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> body) {
        if (!(body.containsKey("email") && body.containsKey("code")))
            return StatusCode.NO_ENOUGH_ARGS.makeErrorResponseEntity();

        String email = body.get("email");
        String code = body.get("code");

        return verificationService.verifyCode(email, code).makeErrorResponseEntity();
    }
}

package com.cbnuccc.cbnuccc.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cbnuccc.cbnuccc.StatusCode;
import com.cbnuccc.cbnuccc.Service.VerificationService;

@RestController
public class EmailController {
    @Autowired
    JavaMailSender javaMailSender;

    @Autowired
    VerificationService verificationService;

    @GetMapping("/email")
    public ResponseEntity<?> sendEmailToVerify(@RequestParam("to") String to) {
        // pre-process
        verificationService.deleteAllExpiredEmails();

        final String code = verificationService.makeCode();

        StatusCode err = verificationService.saveEmailVerification(to, code);
        if (err.checkIsError())
            return err.makeErrorResponseEntity();

        err = verificationService.sendMailCode(to, code);
        if (err.checkIsError())
            return err.makeErrorResponseEntity();

        return StatusCode.NO_ERROR.makeErrorResponseEntity();
    }

    @PostMapping("/email")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> body) {
        // pre-process
        verificationService.deleteAllExpiredEmails();

        if (!(body.containsKey("email") && body.containsKey("code")))
            return StatusCode.NO_ENOUGH_ARGS.makeErrorResponseEntity();

        String email = body.get("email");
        String code = body.get("code");

        return verificationService.verifyCode(email, code).makeErrorResponseEntity();
    }
}

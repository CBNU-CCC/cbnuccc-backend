package com.cbnuccc.cbnuccc.Service;

import java.util.Optional;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.cbnuccc.cbnuccc.Config.MailgunProperties;
import com.cbnuccc.cbnuccc.Model.Verification;
import com.cbnuccc.cbnuccc.Repository.VerificationJpaRepository;
import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.OffsetDateTimeUtil;
import com.cbnuccc.cbnuccc.Util.SecurityUtil;
import com.cbnuccc.cbnuccc.Util.StatusCode;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

@Service
@RequiredArgsConstructor
@EnableScheduling
public class VerificationService {
    private final VerificationJpaRepository verificationJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtil securityUtil;
    private final MailgunProperties mailgunProperties;

    // if the request is expired, it returns true.
    // otherwise, it returns false.
    // also, if the request's email is not on the DB, it returns true.
    private boolean checkExpiredEmailRequest(String email) {
        Optional<Verification> _verification = verificationJpaRepository.findByEmail(email);
        if (_verification.isEmpty())
            return true;

        Verification verification = _verification.get();
        if (verification.getExpireAt().isBefore(OffsetDateTimeUtil.getNow()))
            return true; // expired

        return false;
    }

    // delete all expired tuples a minute.
    // 1000 ms/s * 60 s/min = 60000 ms/min (1 min)
    @Scheduled(fixedRate = 1000 * 60)
    @Transactional
    public void deleteAllExpiredEmails() {
        long countDeletedRows = verificationJpaRepository
                .deleteByExpireAtBeforeAndIsVerifiedFalse(OffsetDateTimeUtil.getNow());

        if (countDeletedRows != 0)
            LogUtil.printBasicInfoLog(LogHeader.SCHEDULED_DELETE_VERIFICATION_RECORD,
                    LogUtil.makeCountKV((int) countDeletedRows));
    }

    // make 6-digit code
    public String makeCode() {
        String result = "";
        for (int i = 0; i < 6; i++) {
            Integer value = (int) (Math.random() * 10);
            result += value.toString();
        }
        return result;
    }

    // send a email with the code.
    public StatusCode sendMailCode(String to, String code) {
        // check if it runs 5 minutes after the last sending.
        // otherwise, should not run.

        if (!checkExpiredEmailRequest(to)) {
            // it is on the DB properly not expired.
            return StatusCode.CANNOT_SEND_EMAIL_WITHIN_5_MINUTES;
        }

        // send a verification email.
        String messageHeader = "안녕하세요!\n충북대학교 CCC입니다.\n아래와 같이 인증 코드를 알려드립니다.";
        String messageCode = "인증 코드: " + code;
        String messageFooter = "위 코드를 아무에게도 공개하지 마세요!\n감사합니다.";
        final String apiKey = mailgunProperties.getKey();
        final String senderDomain = mailgunProperties.getDomain();
        try {
            HttpResponse<JsonNode> request = Unirest
                    .post("https://api.mailgun.net/v3/" + senderDomain + "/messages")
                    .basicAuth("api", apiKey)
                    .queryString("from", "CBNU CCC <postmaster@" + senderDomain + ">")
                    .queryString("to", to)
                    .queryString("subject", "[CBNU CCC] 🌱 회원가입 인증 코드입니다!")
                    .queryString("text",
                            messageHeader + "\n\n" + messageCode + "\n\n" + messageFooter)
                    .asJson();

            // that the status code is 200 means doing right operation.
            if (request.getStatus() != 200)
                return StatusCode.SOMETHING_WENT_WRONG;
        } catch (UnirestException e) {
            LogUtil.printBasicWarnLog(LogHeader.SEND_REGISTRATION_EMAIL, e.getMessage(), null);
            return StatusCode.SOMETHING_WENT_WRONG;
        }
        return StatusCode.NO_ERROR;
    }

    // save the email and the code.
    public StatusCode saveEmailVerification(String email, String code) {
        try {
            Optional<Verification> _verification = verificationJpaRepository.findByEmail(email);
            Verification verification = new Verification();
            if (_verification.isEmpty()) {
                // if there is not given email...
                verification.setEmail(email);
            } else {
                // if there's given email...
                verification = _verification.get();
            }

            verification.setExpireAt(OffsetDateTimeUtil.getNow().plusMinutes(5));
            verification.setCode(passwordEncoder.encode(securityUtil.addPepper(code)));
            verification.setIsVerified(false);

            verificationJpaRepository.save(verification);
            return StatusCode.NO_ERROR;
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.SEND_REGISTRATION_EMAIL, LogUtil.makeExceptionKV(e));
            return StatusCode.SOMETHING_WENT_WRONG;
        }
    }

    // verify the code.
    public StatusCode verifyCode(String email, String code) {
        Optional<Verification> _verification = verificationJpaRepository.findByEmail(email);
        if (_verification.isEmpty())
            return StatusCode.NO_EMAIL_FOUND;
        Verification verification = _verification.get();

        if (verification.getIsVerified())
            return StatusCode.ALREADY_VERIFIED;

        // check that is given code right.
        boolean isRightCode = passwordEncoder.matches(securityUtil.addPepper(code), verification.getCode());
        if (!isRightCode)
            return StatusCode.WRONG_CODE;

        if (checkExpiredEmailRequest(email)) {
            // if expired, delete it.
            try {
                verificationJpaRepository.delete(verification);
            } catch (Exception e) {
                LogUtil.printBasicWarnLog(LogHeader.CONFIRM_REGISTRATION_CODE, LogUtil.makeExceptionKV(e));
                return StatusCode.SOMETHING_WENT_WRONG;
            }
            return StatusCode.REQUEST_IS_EXPIRED;
        }

        // right code and situation.
        verification.setIsVerified(true);

        try {
            verificationJpaRepository.save(verification);
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.CONFIRM_REGISTRATION_CODE, LogUtil.makeExceptionKV(e));
            return StatusCode.SOMETHING_WENT_WRONG;
        }

        return StatusCode.NO_ERROR;
    }
}

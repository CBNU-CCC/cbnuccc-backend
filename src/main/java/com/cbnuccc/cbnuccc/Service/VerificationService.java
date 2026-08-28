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

    // 요청이 만료되었다면 true를 반환
    // 그렇지 않다면 false를 반환
    // 또한, 요청의 이메일이 DB에 없다면 true를 반환
    private boolean checkExpiredEmailRequest(String email) {
        Optional<Verification> _verification = verificationJpaRepository.findByEmail(email.toLowerCase());
        if (_verification.isEmpty())
            return true;

        Verification verification = _verification.get();
        if (verification.getExpireAt().isBefore(OffsetDateTimeUtil.getNow()))
            return true; // 만료됨

        return false;
    }

    // 재요청 가능하다면 true를 반환
    // 그렇지 않다면 false를 반환
    // 또한, 요청의 이메일이 DB에 없다면 true를 반환
    private boolean checkRerequestableEmailRequest(String email) {
        Optional<Verification> _verification = verificationJpaRepository.findByEmail(email.toLowerCase());
        if (_verification.isEmpty())
            return true;

        Verification verification = _verification.get();
        if (verification.getRerequestableAt().isBefore(OffsetDateTimeUtil.getNow()))
            return true; // 재요청 가능

        return false;
    }

    // 1분마다 만료된 모든 튜플 삭제하기
    // 1000 ms/s * 60 s/min = 60000 ms/min (1분)
    @Scheduled(fixedRate = 1000 * 60)
    @Transactional
    public void deleteAllExpiredEmails() {
        long countDeletedRows = verificationJpaRepository
                .deleteByExpireAtBeforeAndIsVerifiedFalse(OffsetDateTimeUtil.getNow());

        if (countDeletedRows != 0)
            LogUtil.printBasicInfoLog(LogHeader.SCHEDULED_DELETE_VERIFICATION_RECORD,
                    LogUtil.makeCountKV((int) countDeletedRows));
    }

    // 6자리 코드 생성하기
    public String makeCode() {
        String result = "";
        for (int i = 0; i < 6; i++) {
            Integer value = (int) (Math.random() * 10);
            result += value.toString();
        }
        return result;
    }

    // 코드와 함께 이메일 전송하기
    public StatusCode sendEmailCode(String to, String code) {
        // 마지막 전송 5분 후에 실행되는지 확인하기
        // 그렇지 않다면 실행하지 않아야 함

        if (!checkRerequestableEmailRequest(to)) {
            // DB에 정상적으로 존재하며 재요청 가능
            return StatusCode.CANNOT_SEND_EMAIL_WITHIN_10_SECONDS;
        }

        // 인증 이메일 전송하기
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

            // 상태 코드가 200이라는 것은 정상적으로 처리됨
            if (request.getStatus() != 200)
                return StatusCode.SOMETHING_WENT_WRONG;
        } catch (UnirestException e) {
            LogUtil.printBasicWarnLog(LogHeader.SEND_REGISTRATION_EMAIL, e.getMessage(), null);
            return StatusCode.SOMETHING_WENT_WRONG;
        }
        return StatusCode.NO_ERROR;
    }

    // 이메일과 코드 저장하기
    public StatusCode saveEmailVerification(String email, String code) {
        email = email.toLowerCase();
        try {
            Optional<Verification> _verification = verificationJpaRepository.findByEmail(email);
            Verification verification = new Verification();
            if (_verification.isEmpty()) {
                // 주어진 이메일이 없다면...
                verification.setEmail(email);
            } else {
                // 주어진 이메일이 있다면...
                verification = _verification.get();
            }

            verification.setExpireAt(OffsetDateTimeUtil.getNow().plusMinutes(5));
            verification.setRerequestableAt(OffsetDateTimeUtil.getNow().plusSeconds(10));
            verification.setCode(passwordEncoder.encode(securityUtil.addPepper(code)));
            verification.setIsVerified(false);

            verificationJpaRepository.save(verification);
            return StatusCode.NO_ERROR;
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.SEND_REGISTRATION_EMAIL, LogUtil.makeExceptionKV(e));
            return StatusCode.SOMETHING_WENT_WRONG;
        }
    }

    // 코드 검증하기
    public StatusCode verifyCode(String email, String code) {
        email = email.toLowerCase();

        Optional<Verification> _verification = verificationJpaRepository.findByEmail(email);
        if (_verification.isEmpty())
            return StatusCode.NO_EMAIL_FOUND;
        Verification verification = _verification.get();

        if (verification.getIsVerified())
            return StatusCode.ALREADY_VERIFIED;

        // 주어진 코드가 올바른지 확인하기
        boolean isRightCode = passwordEncoder.matches(securityUtil.addPepper(code), verification.getCode());
        if (!isRightCode)
            return StatusCode.WRONG_CODE;

        if (checkExpiredEmailRequest(email)) {
            // 만료되었다면 삭제하기
            try {
                verificationJpaRepository.delete(verification);
            } catch (Exception e) {
                LogUtil.printBasicWarnLog(LogHeader.CONFIRM_REGISTRATION_CODE, LogUtil.makeExceptionKV(e));
                return StatusCode.SOMETHING_WENT_WRONG;
            }
            return StatusCode.REQUEST_IS_EXPIRED;
        }

        // 올바른 코드 검증 완료
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

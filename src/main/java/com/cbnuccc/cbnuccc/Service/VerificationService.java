package com.cbnuccc.cbnuccc.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.cbnuccc.cbnuccc.StatusCode;
import com.cbnuccc.cbnuccc.Model.Verification;
import com.cbnuccc.cbnuccc.Repository.VerificationJpaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VerificationService {
    private final JavaMailSender javaMailSender;
    private final VerificationJpaRepository verificationJpaRepository;

    // if the request is expired, it returns true.
    // otherwise, it returns false.
    // also, if the request's email is not on the DB, it returns true.
    private boolean checkExpiredEmailRequest(String email) {
        Optional<Verification> _verification = verificationJpaRepository.findByEmail(email);
        if (_verification.isEmpty())
            return true;

        Verification verification = _verification.get();
        if (verification.getExpireAt().isBefore(OffsetDateTime.now(ZoneId.of("Asia/Seoul"))))
            return true; // expired

        return false;
    }

    // delete all expired tuples.
    @Transactional
    public void deleteAllExpiredEmails() {
        verificationJpaRepository.deleteByExpireAtBeforeAndIsVerifiedFalse(OffsetDateTime.now(ZoneId.of("Asia/Seoul")));
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
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("[CBNU CCC] 🌱 회원가입 인증 코드입니다!");

            String messageHeader = "안녕하세요!\n충북대학교 CCC입니다.\n아래와 같이 인증 코드를 알려드립니다.";
            String messageCode = "인증 코드: " + code;
            String messageFooter = "감사합니다.";
            message.setText(messageHeader + "\n\n" + messageCode + "\n\n" + messageFooter);

            javaMailSender.send(message);
            return StatusCode.NO_ERROR;
        } catch (Exception e) {
            System.err.println(e);
            return StatusCode.SOMETHING_WENT_WRONG;
        }
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

            verification.setExpireAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")).plusMinutes(5));
            verification.setCode(code);
            verification.setIsVerified(false);

            verificationJpaRepository.save(verification);
            return StatusCode.NO_ERROR;
        } catch (Exception e) {
            System.err.println(e);
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

        if (!verification.getCode().equals(code))
            return StatusCode.WRONG_CODE;

        if (checkExpiredEmailRequest(email)) {
            // if expired, delete it.
            try {
                verificationJpaRepository.delete(verification);
            } catch (Exception e) {
                System.err.println(e);
                return StatusCode.SOMETHING_WENT_WRONG;
            }
            return StatusCode.REQUEST_IS_EXPIRED;
        }

        // right code and situation.
        verification.setIsVerified(true);

        try {
            verificationJpaRepository.save(verification);
        } catch (Exception e) {
            System.err.println(e);
            return StatusCode.SOMETHING_WENT_WRONG;
        }

        return StatusCode.NO_ERROR;
    }
}

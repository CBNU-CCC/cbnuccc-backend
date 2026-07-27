package com.cbnuccc.cbnuccc.Util;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import lombok.Getter;

public enum StatusCode {
    // Error codes
    NO_ERROR(HttpStatus.OK, "오류가 없습니다.", -1),
    SOMETHING_WENT_WRONG(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 오류가 발생했습니다.", 0),
    DUPLICATED_EMAIL(HttpStatus.CONFLICT, "주어진 이메일이 중복됩니다.", 1),
    NO_USER_FOUND(HttpStatus.NOT_FOUND, "구하고자 하는 사용자가 존재하지 않습니다.", 2),
    CONNOT_CHANGE_IMPORTANT_INFORMATION(HttpStatus.FORBIDDEN, "사용자의 중요 정보는 수정할 수 없습니다.", 3),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "주어진 JWT 토큰이 유효하지 않습니다.", 4),
    NO_EMAIL_FOUND(HttpStatus.NOT_FOUND, "구하고자 하는 이메일이 존재하지 않습니다.", 5),
    WRONG_CODE(HttpStatus.BAD_REQUEST, "주어진 코드가 잘못되었습니다.", 6),
    REQUEST_IS_EXPIRED(HttpStatus.BAD_REQUEST, "주어진 요청은 만료되었습니다.", 7),
    NO_ENOUGH_ARGS(HttpStatus.BAD_REQUEST, "주어진 인자가 충분하지 않습니다.", 8),
    ALREADY_VERIFIED(HttpStatus.OK, "주어진 코드는 이미 인증되었습니다.", 9),
    NOT_VERIFIED(HttpStatus.FORBIDDEN, "주어진 유저는 유효하지 않습니다.", 10),
    NOT_DUPLICATED_EMAIL(HttpStatus.OK, "주어진 이메일은 중복되지 않습니다.", 11),
    EMPTY_GIVEN_IMAGE(HttpStatus.NOT_FOUND, "주어진 사진이 비어 있습니다.", 12),
    NO_PRAYER_FOUND(HttpStatus.NOT_FOUND, "주어진 기도가 존재하지 않습니다.", 13),
    NO_MISSION_FOUND(HttpStatus.NOT_FOUND, "주어진 선교가 존재하지 않습니다.", 14),
    EXCEED_2MB(HttpStatus.BAD_REQUEST, "압축한 사진의 용량이 2MB를 초과할 수 없습니다.", 15),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "주어진 이메일을 찾을 수 없거나, 계정이 10분 동안 접근할 수 없습니다(locked).", 16),
    CANNOT_SEND_EMAIL_WITHIN_5_MINUTES(HttpStatus.BAD_REQUEST, "이메일 요청을 5분 이내에 다시 요구할 수 없습니다.", 17),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST,
            "주어진 비밀번호가 유효하지 않습니다. (비밀번호는 8~15자리여야 하며, 하나 이상의 특수문자와 숫자가 들어가야 합니다.)",
            18),
    PASSWORD_IS_INCURRECT(HttpStatus.BAD_REQUEST, "주어진 비밀번호가 틀렸습니다.", 19),
    CANNOT_CHANGE_PASSWORD_WITHIN_5_MINUTES(HttpStatus.BAD_REQUEST, "5분 이내에 비밀번호를 재설정할 수 없습니다.", 20);

    @Getter
    private final HttpStatusCode responseStatus;

    @Getter
    private final String errorMessage;

    @Getter
    private final int errorCode;

    StatusCode(HttpStatusCode status, String message, int code) {
        this.responseStatus = status;
        this.errorMessage = message;
        this.errorCode = code;
    }

    // make response entity of when returning an error.
    public ResponseEntity<?> makeErrorResponseEntity() {
        return ResponseEntity.status(responseStatus).body(Map.of(
                "errorCode", errorCode,
                "message", errorMessage));
    }

    public boolean checkIsError() {
        return !responseStatus.equals(HttpStatus.OK);
    }
}

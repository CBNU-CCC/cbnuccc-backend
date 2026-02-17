package com.cbnuccc.cbnuccc.Util;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import lombok.Getter;

public enum StatusCode {
    // Error codes
    NO_ERROR(HttpStatus.OK, "There is no error.", -1),
    SOMETHING_WENT_WRONG(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong.", 0),
    DUPLICATED_EMAIL(HttpStatus.CONFLICT, "Given email is duplicate.", 1),
    NO_USER_FOUND(HttpStatus.NOT_FOUND, "Cannot found matched user.", 2),
    CONNOT_CHANGE_IMPORTANT_INFORMATION(HttpStatus.FORBIDDEN, "Cannot change user's important information.", 3),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Given jwt token is invalid.", 4),
    NO_EMAIL_FOUND(HttpStatus.NOT_FOUND, "Cannot found given email.", 5),
    WRONG_CODE(HttpStatus.BAD_REQUEST, "Given code is wrong.", 6),
    REQUEST_IS_EXPIRED(HttpStatus.BAD_REQUEST, "The request is expired.", 7),
    NO_ENOUGH_ARGS(HttpStatus.BAD_REQUEST, "There are no enough arguments.", 8),
    ALREADY_VERIFIED(HttpStatus.OK, "Given code is already verified.", 9),
    NOT_VERIFIED(HttpStatus.FORBIDDEN, "Given user is not verified.", 10),
    NOT_DUPLICATED_EMAIL(HttpStatus.OK, "Given email is not duplicate.", 11);

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

    // make response entity of when returning an error with a error log.
    public ResponseEntity<?> makeErrorResponseEntityAndPrintLog(UUID uuid, String subHeader) {
        if (this.checkIsError())
            LogUtil.printErrorLog(this, uuid, subHeader);
        else
            LogUtil.printBasicInfoLog(subHeader,
                    String.format("%s - %s", responseStatus.toString(), errorMessage),
                    uuid);

        return ResponseEntity.status(responseStatus).body(Map.of(
                "errorCode", errorCode,
                "message", errorMessage));
    }

    public boolean checkIsError() {
        return !responseStatus.equals(HttpStatus.OK);
    }
}

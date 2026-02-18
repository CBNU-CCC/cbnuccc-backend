package com.cbnuccc.cbnuccc.Util;

import lombok.Getter;

public enum LogHeader {
    ENTER,
    CREATE_USER,
    GET_USER,
    UPDATE_USER,
    DELETE_USER,
    GET_ME,
    CHECK_EMAIL_DUPLICATION,
    UPLOAD_PROFILE_IMAGE,
    DELETE_PROFILE_IMAGE,
    LOGIN,
    SEND_REGISTRATION_EMAIL,
    CONFIRM_REGISTRATION_CODE;

    @Getter
    private final String header;

    LogHeader() {
        String name = this.toString();
        name = name.replaceAll("_", " ");
        this.header = name;
    }
}

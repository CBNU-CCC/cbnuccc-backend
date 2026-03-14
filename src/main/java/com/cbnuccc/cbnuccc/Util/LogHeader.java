package com.cbnuccc.cbnuccc.Util;

import lombok.Getter;

public enum LogHeader {
    ENTER,
    CREATE_USER,
    GET_USER,
    UPDATE_USER,
    UPDATE_USER_PASSWORD,
    DELETE_USER,
    GET_ME,
    CHECK_EMAIL_DUPLICATION,
    UPLOAD_PROFILE_IMAGE,
    DELETE_PROFILE_IMAGE,
    LOGIN,
    SEND_REGISTRATION_EMAIL,
    CONFIRM_REGISTRATION_CODE,
    CREATE_PRAYER,
    GET_PRAYER,
    UPDATE_PRAYER,
    DELETE_PRAYER,
    CREATE_MISSION,
    GET_MISSION,
    UPDATE_MISSION,
    DELETE_MISSION,
    UPLOAD_MISSION_IMAGE,
    DELETE_MISSION_IMAGE,
    COMPRESS_IMAGE,
    GET_MISSION_AUTHOR,
    GET_PRAYER_AUTHOR,
    SCHEDULED_DELETE_VERIFICATION_RECORD,
    SCHEDULED_DELETE_USELESS_LOGIN_RECORD,
    RESET_PASSWORD;

    @Getter
    private final String header;

    LogHeader() {
        String name = this.toString();
        name = name.replaceAll("_", " ");
        this.header = name;
    }
}

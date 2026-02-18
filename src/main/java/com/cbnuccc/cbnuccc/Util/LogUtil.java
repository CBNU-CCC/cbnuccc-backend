package com.cbnuccc.cbnuccc.Util;

import java.util.UUID;

import org.springframework.http.HttpMethod;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogUtil {
    private static String makeUuidString(UUID uuid) {
        String uuidString = "(Anonymous)";
        if (uuid != null)
            uuidString = uuid.toString().substring(0, 8) + "-...";
        return uuidString;
    }

    // print a basic log in INFO LEVEL.
    public static void printBasicInfoLog(LogHeader header, String message, UUID uuid) {
        String uuidString = makeUuidString(uuid);
        log.info("[{}] {}, uuid={}", header.getHeader(), message, uuidString);
    }

    // print a basic log in WARN LEVEL.
    public static void printBasicWarnLog(LogHeader header, String message, UUID uuid) {
        String uuidString = makeUuidString(uuid);
        log.warn("[{}] {}, uuid={}", header.getHeader(), message, uuidString);
    }

    // print a log showing request's method, path and a part of uuid in INFO LEVEL.
    public static void printEnteringLog(HttpMethod method, String path, UUID uuid) {
        String message = String.format("%s %s", method.toString(), path);
        printBasicInfoLog(LogHeader.ENTER, message, uuid);
    }

    // print a log showing an error in WARN LEVEL.
    public static void printErrorLog(LogHeader header, StatusCode code, UUID uuid) {
        String message = String.format("%s - %s", code.getResponseStatus(), code.getErrorMessage());
        printBasicWarnLog(header, message, uuid);
    }
}

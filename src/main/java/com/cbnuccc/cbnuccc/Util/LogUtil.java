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
    public static void printBasicInfoLog(String header, String message, UUID uuid) {
        String uuidString = makeUuidString(uuid);
        log.info("[{}] {}, uuid={}", header, message, uuidString);
    }

    // print a basic log with sub-header in INFO LEVEL.
    public static void printBasicInfoLog(String header, String message, UUID uuid, String subHeader) {
        printBasicInfoLog(header + "/" + subHeader, message, uuid);
    }

    // print a basic log in WARN LEVEL.
    public static void printBasicWarnLog(String header, String message, UUID uuid) {
        String uuidString = makeUuidString(uuid);
        log.warn("[{}] {}, uuid={}", header, message, uuidString);
    }

    // print a basic log with sub-header in WARN LEVEL.
    public static void printBasicWarnLog(String header, String message, UUID uuid, String subHeader) {
        printBasicWarnLog(header + "/" + subHeader, message, uuid);
    }

    // print a log showing request's method, path and a part of uuid in INFO LEVEL.
    public static void printEnteringLog(HttpMethod method, String path, UUID uuid) {
        printBasicInfoLog("ENTERED", String.format("%s %s", method.toString(), path), uuid);
    }

    // print a log showing an error in WARN LEVEL.
    public static void printErrorLog(StatusCode code, UUID uuid) {
        String message = String.format("%s - %s", code.getResponseStatus(), code.getErrorMessage());
        printBasicWarnLog("ERROR", message, uuid);
    }

    // print a log showing an error and sub-header in WARN LEVEL.
    public static void printErrorLog(StatusCode code, UUID uuid, String subHeader) {
        String message = String.format("%s - %s", code.getResponseStatus(), code.getErrorMessage());
        printBasicWarnLog("ERROR", message, uuid, subHeader);
    }
}

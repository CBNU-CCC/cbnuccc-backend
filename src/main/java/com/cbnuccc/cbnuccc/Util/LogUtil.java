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

    // print a log showing request's method, path and a part of uuid in INFO LEVEL.
    public static void printEnteringLog(HttpMethod method, String path, UUID uuid) {
        String uuidString = makeUuidString(uuid);
        log.info("[ENTERED] {} {}, uuid={}", method.toString(), path, uuidString);
    }

    // print a log showing an error in WARN LEVEL.
    public static void printErrorLog(StatusCode code, UUID uuid) {
        String uuidString = makeUuidString(uuid);
        log.warn("[ERROR CODE] {} - {}, uuid={}", code.getResponseStatus(), code.getErrorMessage(), uuidString);
    }
}

package com.cbnuccc.cbnuccc.Util;

import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArgument;

@Slf4j
public class LogUtil {
    public static StructuredArgument makeUuidStringKV(UUID uuid) {
        String uuidString = "(Anonymous)";
        if (uuid != null)
            uuidString = uuid.toString().substring(0, 8);
        return kv("uuid", uuidString);
    }

    public static StructuredArgument makeExceptionKV(Exception e) {
        return kv("error", e.getMessage());
    }

    public static StructuredArgument makeStatusCodeMessageKV(StatusCode code) {
        return kv("code_message", code.getErrorMessage());
    }

    public static StructuredArgument makeCountKV(int count) {
        return kv("count", count);
    }

    public static StructuredArgument makeIdKV(int id) {
        return kv("id", id);
    }

    public static StructuredArgument makeEmailKV(String email) {
        if (email.indexOf("@") < 0)
            return kv("email", "(Wrong email)");

        String result = email.substring(0, email.indexOf("@"));
        return kv("email", result);
    }

    public static void printBasicInfoLog(LogHeader logHeader, Object... kvs) {
        log.info(logHeader.getHeader(), kvs);
    }

    public static void printBasicWarnLog(LogHeader logHeader, Object... kvs) {
        log.warn(logHeader.getHeader(), kvs);
    }
}

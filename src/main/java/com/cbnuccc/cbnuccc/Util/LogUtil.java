package com.cbnuccc.cbnuccc.Util;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

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

    public static StructuredArgument makeIdKV(long id) {
        return kv("id", id);
    }

    public static StructuredArgument makePageSizeKV(Pageable pageable) {
        return kv("size", pageable.getPageSize());
    }

    public static StructuredArgument makePageNumberKV(Pageable pageable) {
        return kv("page", pageable.getPageNumber());
    }

    public static StructuredArgument makeEmailKV(String email) {
        return kv("email", email.hashCode());
    }

    public static void printBasicInfoLog(LogHeader logHeader, Object... kvs) {
        log.info(logHeader.getHeader(), kv("log_uuid", UUID.randomUUID()), kvs);
    }

    public static void printBasicWarnLog(LogHeader logHeader, Object... kvs) {
        log.warn(logHeader.getHeader(), kv("log_uuid", UUID.randomUUID()), kvs);
    }
}

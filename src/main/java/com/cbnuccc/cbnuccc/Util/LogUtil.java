package com.cbnuccc.cbnuccc.Util;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import static net.logstash.logback.argument.StructuredArguments.entries;
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

    // StatusCode 하나로 error_code, http_status, error_message를 한 번에 구조화된 필드로 남기기
    // (예전엔 error_message만 남아서 로그만 보고 어떤 오류 코드였는지 특정/집계할 수 없었음)
    public static StructuredArgument makeStatusCodeMessageKV(StatusCode code) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("error_code", code.getErrorCode());
        fields.put("http_status", code.getResponseStatus().value());
        fields.put("error_message", code.getErrorMessage());
        return entries(fields);
    }

    public static StructuredArgument makeCountKV(int count) {
        return kv("count", count);
    }

    public static StructuredArgument makeIdKV(long id) {
        return kv("id", id);
    }

    public static StructuredArgument makeRecordDateKV(LocalDate recordDate) {
        return kv("record_date", recordDate);
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

    public static StructuredArgument makeHttpStatusKV(int status) {
        return kv("status", status);
    }

    public static StructuredArgument makeDurationKV(long durationMillis) {
        return kv("duration_ms", durationMillis);
    }

    // 정상 흐름(성공) 로그
    public static void printBasicInfoLog(LogHeader logHeader, Object... kvs) {
        log.info(logHeader.getHeader(), withLogUuid(kvs));
    }

    // 예상 가능한 업무상 실패(입력 오류, 중복, 권한 없음 등 - StatusCode로 표현되는 것들) 로그
    public static void printBasicWarnLog(LogHeader logHeader, Object... kvs) {
        log.warn(logHeader.getHeader(), withLogUuid(kvs));
    }

    // 예상 가능하지만(비밀번호 오류, 만료/위조된 토큰 등) 예외 자체의 상세 정보(스택 트레이스 등)도
    // 함께 남기고 싶을 때 - 버그는 아니므로 WARN 레벨 유지
    public static void printBasicWarnLog(LogHeader logHeader, Throwable throwable, Object... kvs) {
        log.warn(logHeader.getHeader(), withLogUuidAndThrowable(throwable, kvs));
    }

    // 예상치 못한 오류(버그, DB/외부 연동 실패 등) - 반드시 ERROR 레벨 + 스택 트레이스와 함께 기록
    public static void printBasicErrorLog(LogHeader logHeader, Throwable throwable, Object... kvs) {
        log.error(logHeader.getHeader(), withLogUuidAndThrowable(throwable, kvs));
    }

    // varargs로 받은 kvs를 log_uuid와 함께 "하나의 평평한 배열"로 합쳐서 SLF4J에 전달하기.
    // 주의: log.info(header, kv(...), kvs)처럼 kv(...)와 kvs를 나란히 넘기면, kvs(Object[])가
    // 그 자체로 인자 배열의 한 원소로 중첩되어 버려서 kvs에 담긴 구조화 필드가 로그에 전혀 찍히지 않는다
    // (실제로 확인된 버그 - 지금까지 대부분의 로그 호출부에서 넘긴 kv들이 조용히 유실되고 있었음).
    // 반드시 이렇게 하나의 배열로 합쳐서 넘겨야 SLF4J/LogstashEncoder가 각 kv를 개별 필드로 펼쳐 씀
    private static Object[] withLogUuid(Object... kvs) {
        int length = kvs == null ? 0 : kvs.length;
        Object[] combined = new Object[length + 1];
        combined[0] = kv("log_uuid", UUID.randomUUID());
        if (length > 0)
            System.arraycopy(kvs, 0, combined, 1, length);
        return combined;
    }

    // 위와 동일하되, 예외를 배열의 마지막 원소로 추가함 - SLF4J는 인자 배열의 마지막 원소가
    // Throwable이면 이를 일반 포맷 인자가 아닌 로그 이벤트의 예외(스택 트레이스 포함)로 인식함
    private static Object[] withLogUuidAndThrowable(Throwable throwable, Object... kvs) {
        int length = kvs == null ? 0 : kvs.length;
        Object[] combined = new Object[length + 2];
        combined[0] = kv("log_uuid", UUID.randomUUID());
        if (length > 0)
            System.arraycopy(kvs, 0, combined, 1, length);
        combined[combined.length - 1] = throwable;
        return combined;
    }
}

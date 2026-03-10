package com.cbnuccc.cbnuccc.Util;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public class OffsetDateTimeUtil {
    public static OffsetDateTime getNow() {
        return OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}

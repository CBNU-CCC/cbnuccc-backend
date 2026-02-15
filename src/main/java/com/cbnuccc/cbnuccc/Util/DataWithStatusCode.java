package com.cbnuccc.cbnuccc.Util;

public record DataWithStatusCode<T>(StatusCode code, T data) {
}

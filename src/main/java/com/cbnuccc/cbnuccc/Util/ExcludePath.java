package com.cbnuccc.cbnuccc.Util;

import org.springframework.http.HttpMethod;

public record ExcludePath(HttpMethod method, String uriPattern) {
}
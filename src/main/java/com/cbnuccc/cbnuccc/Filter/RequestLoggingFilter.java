package com.cbnuccc.cbnuccc.Filter;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.SecurityUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// 모든 요청을 감싸서(인증 제외 목록 여부와 무관하게 항상 실행됨) 요청 단위 상관관계(request_id)를
// MDC에 심고, 요청이 끝나면 상태 코드/소요 시간을 담은 완료 로그를 정확히 한 줄 남기는 필터.
// MDC의 생성과 정리(clear)를 이 필터 하나가 전담해야 하며, 체인 안쪽의 다른 필터(JwtFilter 등)가
// 자체적으로 MDC.clear()를 호출하면 이 필터가 심어둔 request_id 등이 완료 로그가 찍히기도 전에
// 사라져 버리므로 절대 안 됨 - MDC는 항상 "가장 바깥쪽에서 감싼 필터"만 정리해야 함
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    // Spring Boot가 이 필터를 서블릿 컨테이너에 한 번 더 자동 등록하지 않도록 막기
    // (SecurityConfig에서 Spring Security 필터 체인의 맨 앞쪽에 명시적으로 등록함)
    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> disableRequestLoggingFilter(RequestLoggingFilter filter) {
        FilterRegistrationBean<RequestLoggingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        long startedAtMillis = System.currentTimeMillis();

        MDC.put("request_id", requestId);
        MDC.put("method", request.getMethod());
        MDC.put("endpoint", request.getRequestURI());
        MDC.put("ip", SecurityUtil.getClientIp(request));

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMillis = System.currentTimeMillis() - startedAtMillis;
            int status = response.getStatus();

            // 4xx/5xx로 끝난 요청은 눈에 띄도록 WARN으로, 나머지는 INFO로 완료 로그 남기기
            if (status >= 400)
                LogUtil.printBasicWarnLog(LogHeader.REQUEST_COMPLETED,
                        LogUtil.makeHttpStatusKV(status), LogUtil.makeDurationKV(durationMillis));
            else
                LogUtil.printBasicInfoLog(LogHeader.REQUEST_COMPLETED,
                        LogUtil.makeHttpStatusKV(status), LogUtil.makeDurationKV(durationMillis));

            MDC.clear();
        }
    }
}

package com.cbnuccc.cbnuccc.Config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.StatusCode;

// 어떤 컨트롤러/서비스에서도 잡히지 않고 빠져나온 모든 예외를 이 지점에서 반드시 붙잡아,
// 이 앱의 다른 로그와 동일한 구조적 형식(ERROR 레벨 + 전체 스택 트레이스 + 요청 상관관계 MDC)으로
// 남기고, 클라이언트에게는 다른 오류 응답과 동일한 모양({"errorCode":...,"message":...})으로 응답한다.
// 이게 없으면 예외가 이 앱의 로깅 파이프라인을 완전히 벗어나 원인 파악이 사실상 불가능해진다.
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnhandledException(Exception e) {
        LogUtil.printBasicErrorLog(LogHeader.UNHANDLED_EXCEPTION, e);
        return StatusCode.SOMETHING_WENT_WRONG.makeErrorResponseEntity();
    }
}

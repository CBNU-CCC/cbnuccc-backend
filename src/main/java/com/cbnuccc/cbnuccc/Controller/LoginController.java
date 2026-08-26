package com.cbnuccc.cbnuccc.Controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.cbnuccc.cbnuccc.Dto.LoginDto;
import com.cbnuccc.cbnuccc.Dto.TokenDto;
import com.cbnuccc.cbnuccc.Service.LoginService;
import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.SecurityUtil;
import com.cbnuccc.cbnuccc.Util.StatusCode;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@Tag(name="Login Controller", description = "로그인 시의 기능")
@RestController
@RequiredArgsConstructor
public class LoginController {
    private final LoginService loginService;
    private final SecurityUtil securityUtil;

    @Operation(summary="로그인")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "로그인 성공 (인증 토큰 발급)",
            content = @Content(schema = @Schema(implementation = TokenDto.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "잘못된 이메일/비밀번호 입력 또는 인자 누락",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "계정 잠금 상태 (로그인 시도 횟수 초과) 또는 로그인 거부",
            content = @Content
        )
    })
    @PostMapping("/login")
    public ResponseEntity<?> loginJWT(@Parameter(hidden = true) HttpServletRequest request, @RequestBody LoginDto data) {
        // 로그인
        String ip = SecurityUtil.getClientIp(request);
        String email = data.getEmail().toLowerCase();
        TokenDto tokenDto = loginService.login(email, data.getPassword(), ip);
        if (tokenDto == null) {
            // 예외 상황 처리
            StatusCode code = loginService.recordLoginFailure(email, ip);

            if (code.checkIsError()) {
                LogUtil.printBasicWarnLog(LogHeader.LOGIN, LogUtil.makeStatusCodeMessageKV(code));
                return code.makeErrorResponseEntity();
            }

            if (!loginService.checkLoginable(email, ip))
                return StatusCode.ACCOUNT_LOCKED.makeErrorResponseEntity();

            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 새로 생성된 토큰의 uuid 추출 및 로그 출력
        UUID uuid = UUID.fromString(securityUtil.extractToken(tokenDto.getToken()).get("uuid").toString());
        LogUtil.printBasicInfoLog(LogHeader.LOGIN, LogUtil.makeUuidStringKV(uuid));

        return ResponseEntity.ok(tokenDto);
    }
}

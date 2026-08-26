package com.cbnuccc.cbnuccc.Controller;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.cbnuccc.cbnuccc.Dto.RefreshTokenDto;
import com.cbnuccc.cbnuccc.Dto.TokenDto;
import com.cbnuccc.cbnuccc.Model.MyUser;
import com.cbnuccc.cbnuccc.Model.RefreshToken;
import com.cbnuccc.cbnuccc.Repository.UserJpaRepository;
import com.cbnuccc.cbnuccc.Service.LoginService;
import com.cbnuccc.cbnuccc.Service.RefreshTokenService;
import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.StatusCode;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@Tag(name="Refresh Controller", description = "토큰 갱신 및 로그아웃 관련 기능")
@RestController
@RequiredArgsConstructor
public class RefreshController {
    private final RefreshTokenService refreshTokenService;
    private final LoginService loginService;
    private final UserJpaRepository userJpaRepository;

    @Operation(summary="access token 갱신")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "갱신 성공 (새 access token, refresh token 발급)",
            content = @Content(schema = @Schema(implementation = TokenDto.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "주어진 refresh token이 유효하지 않음(만료 또는 존재하지 않음)",
            content = @Content
        )
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenDto data) {
        Optional<RefreshToken> _refreshToken = refreshTokenService.validate(data.getRefreshToken());
        if (_refreshToken.isEmpty()) {
            LogUtil.printBasicWarnLog(LogHeader.REFRESH_TOKEN,
                    LogUtil.makeStatusCodeMessageKV(StatusCode.INVALID_REFRESH_TOKEN));
            return StatusCode.INVALID_REFRESH_TOKEN.makeErrorResponseEntity();
        }

        Long userId = _refreshToken.get().getUserId();
        Optional<MyUser> _user = userJpaRepository.findById(userId);
        if (_user.isEmpty()) {
            LogUtil.printBasicWarnLog(LogHeader.REFRESH_TOKEN,
                    LogUtil.makeStatusCodeMessageKV(StatusCode.INVALID_REFRESH_TOKEN));
            return StatusCode.INVALID_REFRESH_TOKEN.makeErrorResponseEntity();
        }
        MyUser user = _user.get();

        String newAccessToken = loginService.createAccessToken(user);
        String newRefreshToken = refreshTokenService.rotate(data.getRefreshToken(), userId);
        TokenDto tokenDto = new TokenDto(newAccessToken, newRefreshToken);

        LogUtil.printBasicInfoLog(LogHeader.REFRESH_TOKEN, LogUtil.makeUuidStringKV(user.getUuid()));

        return ResponseEntity.ok(tokenDto);
    }

    @Operation(summary="로그아웃 (refresh token 폐기)")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "로그아웃 성공",
            content = @Content
        )
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody RefreshTokenDto data) {
        refreshTokenService.revoke(data.getRefreshToken());

        LogUtil.printBasicInfoLog(LogHeader.LOGOUT, (Object[]) null);

        return ResponseEntity.ok().build();
    }
}

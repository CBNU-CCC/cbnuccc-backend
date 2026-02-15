package com.cbnuccc.cbnuccc.Controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.cbnuccc.cbnuccc.Dto.TokenDto;
import com.cbnuccc.cbnuccc.Service.LoginService;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.SecurityUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class LoginController {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final LoginService loginService;
    private final SecurityUtil securityUtil;

    @PostMapping("/login")
    public ResponseEntity<?> loginJWT(@RequestBody Map<String, String> data) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                data.get("email"), securityUtil.addPepper(data.get("password")));

        Authentication auth = authenticationManagerBuilder.getObject().authenticate(authToken);

        String token = loginService.createToken(auth, data.get("email"));
        TokenDto tokenDto = new TokenDto(token);

        LogUtil.printBasicInfoLog("LOGIN", "successfully logged-in", UUID.fromString(data.get("uuid")));
        return ResponseEntity.ok(tokenDto);
    }
}

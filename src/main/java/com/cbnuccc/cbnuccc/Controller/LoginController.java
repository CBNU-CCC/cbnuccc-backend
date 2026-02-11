package com.cbnuccc.cbnuccc.Controller;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.cbnuccc.cbnuccc.SecurityUtil;
import com.cbnuccc.cbnuccc.Dto.TokenDto;
import com.cbnuccc.cbnuccc.Service.LoginService;

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

        return ResponseEntity.ok(tokenDto);
    }
}

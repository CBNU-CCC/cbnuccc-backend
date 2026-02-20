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
import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.SecurityUtil;
import com.cbnuccc.cbnuccc.Util.StatusCode;

import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;

// import static net.logstash.logback.argument.StructuredArguments.*;

@RestController
@RequiredArgsConstructor
// @Slf4j
public class LoginController {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final LoginService loginService;
    private final SecurityUtil securityUtil;

    @PostMapping("/login")
    public ResponseEntity<?> loginJWT(@RequestBody Map<String, String> data) {
        // check enough body
        if (!(data.containsKey("email") && data.containsKey("password")))
            return StatusCode.NO_ENOUGH_ARGS.makeErrorResponseEntityAndPrintLog(LogHeader.LOGIN, null);

        // set variables of auth information.
        String email = data.get("email");
        String password = data.get("password");
        String pepperedPassword = securityUtil.addPepper(password);
        boolean rememberMe = Boolean.parseBoolean(data.get("rememberMe")); // default value is false

        // create user's token
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                email, pepperedPassword);
        Authentication auth = authenticationManagerBuilder.getObject().authenticate(authToken);
        String token = loginService.createToken(auth, data.get("email"), rememberMe);
        TokenDto tokenDto = new TokenDto(token);

        // get uuid from created token and print log
        UUID uuid = UUID.fromString(securityUtil.extractToken(token).get("uuid").toString());
        LogUtil.printBasicInfoLog(LogHeader.LOGIN, "successfully logged-in", uuid);

        return ResponseEntity.ok(tokenDto);
    }
}

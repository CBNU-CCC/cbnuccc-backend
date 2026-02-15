package com.cbnuccc.cbnuccc.Controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.cbnuccc.cbnuccc.Dto.LimitedUserDto;
import com.cbnuccc.cbnuccc.Dto.UserDto;
import com.cbnuccc.cbnuccc.Model.MyUser;
import com.cbnuccc.cbnuccc.Service.UserService;
import com.cbnuccc.cbnuccc.Util.DataWithStatusCode;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.StatusCode;

@RestController
public class UserController {
    @Autowired
    private UserService userService;

    // main page
    @GetMapping("/")
    public String home() {
        return "Hello!\nI am okay!\nYou found this... r u a programmer? haha.";
    }

    // get users
    @GetMapping("/user")
    public ResponseEntity<List<LimitedUserDto>> getUser(@ModelAttribute LimitedUserDto userDto) {
        List<LimitedUserDto> dtos = userService.findAllMatchedLimitedUserDtos(userDto);
        String log = String.format("successfully got %d users", dtos.size());
        LogUtil.printBasicInfoLog("GOT USER", log, null);
        return ResponseEntity.ok(dtos);
    }

    // get a user by uuid
    // @Deprecated
    // @GetMapping("/user/{uuid}")
    // public ResponseEntity<?> getUserByUuid(@PathVariable("uuid") UUID uuid) {
    // LimitedUserDto user = new LimitedUserDto();
    // user.setUuid(uuid);

    // List<LimitedUserDto> resultBody = (List<LimitedUserDto>)
    // getUser(user).getBody();
    // if (resultBody.size() == 0)
    // return StatusCode.NO_USER_FOUND.makeErrorResponseEntityAndPrintLog(uuid, "GOT
    // USER");

    // LogUtil.printBasicInfoLog("GOT USER", "successfully got a user", uuid);
    // LimitedUserDto result = resultBody.get(0);
    // return ResponseEntity.ok(result);
    // }

    // get my user data
    @GetMapping("/me")
    public ResponseEntity<?> getMyUserData(Authentication authentication) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        Optional<UserDto> _me = userService.findUserDtoByUuid(uuid);
        if (_me.isEmpty())
            return StatusCode.NO_USER_FOUND.makeErrorResponseEntityAndPrintLog(uuid, "GOT USER");
        UserDto me = _me.get();
        LogUtil.printBasicInfoLog("GOT USER", "successfully got my data", uuid, "ME");
        return ResponseEntity.ok(me);
    }

    // create user, but the user's email should not be same with other's email.
    @PostMapping("/user")
    public ResponseEntity<?> createUser(@RequestBody MyUser user) {
        DataWithStatusCode<LimitedUserDto> result = userService.createUser(user);
        StatusCode code = result.code();
        if (code.checkIsError())
            return code.makeErrorResponseEntityAndPrintLog(null, "CREATED USER");
        LogUtil.printBasicInfoLog("CREATED USER", "successfully created a user", null);
        return ResponseEntity.ok(result.data());
    }

    // update user by uuid
    @PatchMapping("/user")
    public ResponseEntity<?> updateUser(Authentication authentication, @RequestBody MyUser user) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        StatusCode resultCode = userService.updateUserByUuid(uuid, user);
        if (resultCode.checkIsError())
            return resultCode.makeErrorResponseEntityAndPrintLog(uuid, "PATCHED USER");

        LogUtil.printBasicInfoLog("PATCHED USER", "successfully updated a user", uuid);
        return getMyUserData(authentication);
    }

    // delete a user by uuid
    @DeleteMapping("/user")
    public ResponseEntity<?> deleteUser(Authentication authentication) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        ResponseEntity<?> _deletedUser = getMyUserData(authentication);

        StatusCode resultCode = userService.deleteUserByUuid(uuid);
        if (resultCode.checkIsError() || _deletedUser.getStatusCode() != HttpStatus.OK)
            return resultCode.makeErrorResponseEntityAndPrintLog(uuid, "DELETED USER");

        UserDto deletedUser = (UserDto) _deletedUser.getBody();
        LogUtil.printBasicInfoLog("DELETED USER", "successfully deleted a user", uuid);
        return ResponseEntity.ok(deletedUser);
    }
}

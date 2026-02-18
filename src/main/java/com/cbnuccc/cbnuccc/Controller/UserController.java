package com.cbnuccc.cbnuccc.Controller;

import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.cbnuccc.cbnuccc.Dto.LimitedUserDto;
import com.cbnuccc.cbnuccc.Dto.UserDto;
import com.cbnuccc.cbnuccc.Model.MyUser;
import com.cbnuccc.cbnuccc.Service.UserService;
import com.cbnuccc.cbnuccc.Util.DataWithStatusCode;
import com.cbnuccc.cbnuccc.Util.LogHeader;
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
        List<LimitedUserDto> dtos = userService.findAllLimitedUserDtosByLimitedUserDto(userDto);
        String log = String.format("successfully got %d users", dtos.size());
        LogUtil.printBasicInfoLog(LogHeader.GET_USER, log, null);
        return ResponseEntity.ok(dtos);
    }

    // get a user by uuid
    @GetMapping("/user/{uuid}")
    public ResponseEntity<?> getUserByUuid(@PathVariable("uuid") UUID uuid) {
        LimitedUserDto user = new LimitedUserDto();
        user.setUuid(uuid);

        List<LimitedUserDto> resultBody = (List<LimitedUserDto>) getUser(user).getBody();
        if (resultBody.size() == 0)
            return StatusCode.NO_USER_FOUND.makeErrorResponseEntityAndPrintLog(LogHeader.GET_USER, uuid);

        LogUtil.printBasicInfoLog(LogHeader.GET_USER, "successfully got a user", uuid);
        LimitedUserDto result = resultBody.get(0);
        return ResponseEntity.ok(result);
    }

    // get my user data
    @GetMapping("/me")
    public ResponseEntity<?> getMyUserData(Authentication authentication) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        Optional<UserDto> _me = userService.findUserDtoByUuid(uuid);
        if (_me.isEmpty())
            return StatusCode.NO_USER_FOUND.makeErrorResponseEntityAndPrintLog(LogHeader.GET_USER, uuid);
        UserDto me = _me.get();
        LogUtil.printBasicInfoLog(LogHeader.GET_USER, "successfully got my data", uuid);
        return ResponseEntity.ok(me);
    }

    // check email duplication
    @GetMapping("/email-duplication")
    public ResponseEntity<?> checkEmailDuplication(@RequestBody Map<String, String> body) {
        if (!body.containsKey("email"))
            return StatusCode.NO_ENOUGH_ARGS.makeErrorResponseEntityAndPrintLog(
                    LogHeader.CHECK_EMAIL_DUPLICATION, null);
        String email = body.get("email");

        Optional<UserDto> _user = userService.findUserDtoByEmail(email);
        if (_user.isPresent())
            return StatusCode.DUPLICATED_EMAIL.makeErrorResponseEntityAndPrintLog(LogHeader.CHECK_EMAIL_DUPLICATION,
                    null);
        return StatusCode.NOT_DUPLICATED_EMAIL.makeErrorResponseEntityAndPrintLog(LogHeader.CHECK_EMAIL_DUPLICATION,
                null);
    }

    // create user, but the user's email should not be same with other's email.
    @PostMapping("/user")
    public ResponseEntity<?> createUser(@RequestBody MyUser user) {
        DataWithStatusCode<LimitedUserDto> result = userService.createUser(user);
        StatusCode code = result.code();
        if (code.checkIsError())
            return code.makeErrorResponseEntityAndPrintLog(LogHeader.CREATE_USER, null);
        LogUtil.printBasicInfoLog(LogHeader.CREATE_USER, "successfully created a user", null);
        return ResponseEntity.ok(result.data());
    }

    // update user by uuid
    @PatchMapping("/user")
    public ResponseEntity<?> updateUser(Authentication authentication, @RequestBody MyUser user) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        StatusCode resultCode = userService.updateUserByUuid(uuid, user);
        if (resultCode.checkIsError())
            return resultCode.makeErrorResponseEntityAndPrintLog(LogHeader.UPDATE_USER, uuid);

        LogUtil.printBasicInfoLog(LogHeader.UPDATE_USER, "successfully updated a user", uuid);
        return getMyUserData(authentication);
    }

    // delete a user by uuid
    @DeleteMapping("/user")
    public ResponseEntity<?> deleteUser(Authentication authentication) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        ResponseEntity<?> _deletedUser = getMyUserData(authentication);

        StatusCode resultCode = userService.deleteUserByUuid(uuid);
        if (resultCode.checkIsError() || _deletedUser.getStatusCode() != HttpStatus.OK)
            return resultCode.makeErrorResponseEntityAndPrintLog(LogHeader.DELETE_USER, uuid);

        UserDto deletedUser = (UserDto) _deletedUser.getBody();
        LogUtil.printBasicInfoLog(LogHeader.DELETE_USER, "successfully deleted a user", uuid);
        return ResponseEntity.ok(deletedUser);
    }

    // upload given user's profile image by uuid (upsert)
    @PostMapping("/profile-image")
    public ResponseEntity<?> uploadProfileImage(Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        Optional<UserDto> _user = userService.findUserDtoByUuid(uuid);

        if (_user.isEmpty())
            return StatusCode.NO_USER_FOUND.makeErrorResponseEntityAndPrintLog(LogHeader.UPLOAD_PROFILE_IMAGE, uuid);
        if (file.isEmpty())
            return StatusCode.EMPTY_GIVEN_IMAGE.makeErrorResponseEntityAndPrintLog(LogHeader.UPLOAD_PROFILE_IMAGE,
                    uuid);

        return userService.uploadProfileImage(file, uuid)
                .makeErrorResponseEntityAndPrintLog(LogHeader.UPLOAD_PROFILE_IMAGE, uuid);
    }

    // delete given user's profile image by uuid
    @DeleteMapping("/profile-image")
    public ResponseEntity<?> deleteProfileImage(Authentication authentication) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        Optional<UserDto> _user = userService.findUserDtoByUuid(uuid);
        if (_user.isEmpty())
            return StatusCode.NO_USER_FOUND.makeErrorResponseEntityAndPrintLog(LogHeader.DELETE_PROFILE_IMAGE, uuid);

        return userService.deleteProfileImage(uuid).makeErrorResponseEntityAndPrintLog(LogHeader.DELETE_PROFILE_IMAGE,
                uuid);
    }
}

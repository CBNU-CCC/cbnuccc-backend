package com.cbnuccc.cbnuccc.Controller;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.cbnuccc.cbnuccc.Dto.LimitedUserDto;
import com.cbnuccc.cbnuccc.Dto.OldAndNewPasswordDto;
import com.cbnuccc.cbnuccc.Dto.ResetPasswordDto;
import com.cbnuccc.cbnuccc.Dto.UserDto;
import com.cbnuccc.cbnuccc.Model.MyUser;
import com.cbnuccc.cbnuccc.Service.UserService;
import com.cbnuccc.cbnuccc.Util.DataWithStatusCode;
import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.PaginationUtil;
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
    public ResponseEntity<Object> getUser(@RequestBody(required = false) LimitedUserDto userDto, Pageable pageable) {
        if (userDto == null)
            userDto = new LimitedUserDto();

        Page<LimitedUserDto> dtos = userService.findAllLimitedUserDtosByLimitedUserDto(userDto, pageable);
        LogUtil.printBasicInfoLog(LogHeader.GET_USER,
                LogUtil.makeCountKV(dtos.getNumberOfElements()),
                LogUtil.makePageNumberKV(pageable),
                LogUtil.makePageSizeKV(pageable));
        return ResponseEntity.ok(PaginationUtil.makePaginationMap(dtos));
    }

    // get a user by uuid
    @GetMapping("/user/{uuid}")
    public ResponseEntity<?> getUserByUuid(@PathVariable("uuid") UUID uuid) {
        LimitedUserDto user = new LimitedUserDto();
        user.setUuid(uuid);

        Page<LimitedUserDto> resultBody = userService.findAllLimitedUserDtosByLimitedUserDto(user, Pageable.ofSize(1));
        if (resultBody.getSize() == 0) {
            LogUtil.printBasicWarnLog(LogHeader.GET_USER, LogUtil.makeStatusCodeMessageKV(StatusCode.NO_USER_FOUND));
            return StatusCode.NO_USER_FOUND.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.GET_USER, LogUtil.makeCountKV(resultBody.getNumberOfElements()));
        LimitedUserDto result = resultBody.toList().get(0);
        return ResponseEntity.ok(result);
    }

    // get my user data
    @GetMapping("/me")
    public ResponseEntity<?> getMyUserData(Authentication authentication) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        Optional<UserDto> _me = userService.findUserDtoByUuid(uuid);
        if (_me.isEmpty()) {
            LogUtil.printBasicWarnLog(LogHeader.GET_ME, LogUtil.makeStatusCodeMessageKV(StatusCode.NO_USER_FOUND));
            return StatusCode.NO_USER_FOUND.makeErrorResponseEntity();
        }
        UserDto me = _me.get();

        LogUtil.printBasicInfoLog(LogHeader.GET_ME, (Object[]) null);
        return ResponseEntity.ok(me);
    }

    // check email duplication
    @GetMapping("/email-duplication")
    public ResponseEntity<?> checkEmailDuplication(@RequestBody Map<String, String> body) {
        if (!body.containsKey("email")) {
            LogUtil.printBasicWarnLog(LogHeader.CHECK_EMAIL_DUPLICATION,
                    LogUtil.makeStatusCodeMessageKV(StatusCode.NO_ENOUGH_ARGS));
            return StatusCode.NO_ENOUGH_ARGS.makeErrorResponseEntity();
        }
        String email = body.get("email");

        Optional<UserDto> _user = userService.findUserDtoByEmail(email);
        if (_user.isPresent()) {
            LogUtil.printBasicWarnLog(LogHeader.CHECK_EMAIL_DUPLICATION, LogUtil.makeStatusCodeMessageKV(
                    StatusCode.DUPLICATED_EMAIL));
            return StatusCode.DUPLICATED_EMAIL.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.CHECK_EMAIL_DUPLICATION, LogUtil.makeEmailKV(email));
        return StatusCode.NOT_DUPLICATED_EMAIL.makeErrorResponseEntity();
    }

    // create user, but the user's email should not be same with other's email.
    @PostMapping("/user")
    public ResponseEntity<?> createUser(@RequestBody MyUser user) {
        DataWithStatusCode<LimitedUserDto> result = userService.createUser(user);
        StatusCode code = result.code();
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.CREATE_USER, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.CREATE_USER, LogUtil.makeUuidStringKV(result.data().getUuid()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result.data());
    }

    // update user by uuid
    @PatchMapping("/user")
    public ResponseEntity<?> updateUser(Authentication authentication, @RequestBody MyUser user) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        StatusCode code = userService.updateUserByUuid(uuid, user);
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.UPDATE_USER, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.UPDATE_USER, LogUtil.makeUuidStringKV(uuid));
        return getMyUserData(authentication);
    }

    // update user by uuid
    @PatchMapping("/user/password")
    public ResponseEntity<?> updateUserPassword(Authentication authentication,
            @RequestBody OldAndNewPasswordDto passwords) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        StatusCode code = userService.updateUserPasswordByUuid(uuid, passwords);
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.UPDATE_USER_PASSWORD, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.UPDATE_USER_PASSWORD, LogUtil.makeUuidStringKV(uuid));
        return getMyUserData(authentication);
    }

    @PatchMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordDto resetPasswordDto) {
        StatusCode code = userService.resetPassword(resetPasswordDto);
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.RESET_PASSWORD, LogUtil.makeEmailKV(resetPasswordDto.getEmail()));
            return code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.RESET_PASSWORD, LogUtil.makeEmailKV(resetPasswordDto.getEmail()));
        return code.makeErrorResponseEntity();
    }

    // delete a user by uuid
    @DeleteMapping("/user")
    public ResponseEntity<?> deleteUser(Authentication authentication) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        ResponseEntity<?> _deletedUser = getMyUserData(authentication);

        StatusCode code = userService.deleteUserByUuid(uuid);
        if (code.checkIsError() || _deletedUser.getStatusCode() != HttpStatus.OK) {
            LogUtil.printBasicWarnLog(LogHeader.DELETE_USER, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }

        UserDto deletedUser = (UserDto) _deletedUser.getBody();
        LogUtil.printBasicInfoLog(LogHeader.DELETE_USER, LogUtil.makeUuidStringKV(uuid));
        return ResponseEntity.ok(deletedUser);
    }

    // upload given user's profile image by uuid (upsert)
    @PostMapping("/profile-image")
    public ResponseEntity<?> uploadProfileImage(Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        Optional<UserDto> _user = userService.findUserDtoByUuid(uuid);

        if (_user.isEmpty()) {
            LogUtil.printBasicWarnLog(LogHeader.UPLOAD_PROFILE_IMAGE,
                    LogUtil.makeStatusCodeMessageKV(StatusCode.NO_USER_FOUND));
            return StatusCode.NO_USER_FOUND.makeErrorResponseEntity();
        }
        if (file.isEmpty()) {
            LogUtil.printBasicWarnLog(LogHeader.UPLOAD_PROFILE_IMAGE, LogUtil.makeStatusCodeMessageKV(
                    StatusCode.EMPTY_GIVEN_IMAGE));
            return StatusCode.EMPTY_GIVEN_IMAGE.makeErrorResponseEntity();
        }

        StatusCode code = userService.uploadProfileImage(file, uuid);
        if (code.checkIsError())
            LogUtil.printBasicWarnLog(LogHeader.UPLOAD_PROFILE_IMAGE, LogUtil.makeStatusCodeMessageKV(code));
        else
            LogUtil.printBasicInfoLog(LogHeader.UPLOAD_PROFILE_IMAGE, (Object[]) null);
        return code.makeErrorResponseEntity();
    }

    // delete given user's profile image by uuid
    @DeleteMapping("/profile-image")
    public ResponseEntity<?> deleteProfileImage(Authentication authentication) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        Optional<UserDto> _user = userService.findUserDtoByUuid(uuid);
        if (_user.isEmpty()) {
            LogUtil.printBasicWarnLog(LogHeader.DELETE_PROFILE_IMAGE,
                    LogUtil.makeStatusCodeMessageKV(StatusCode.NO_USER_FOUND));
            return StatusCode.NO_USER_FOUND.makeErrorResponseEntity();
        }

        StatusCode code = userService.deleteProfileImage(uuid);
        if (code.checkIsError())
            LogUtil.printBasicWarnLog(LogHeader.DELETE_PROFILE_IMAGE, LogUtil.makeStatusCodeMessageKV(code));
        else
            LogUtil.printBasicInfoLog(LogHeader.DELETE_PROFILE_IMAGE, (Object[]) null);
        return code.makeErrorResponseEntity();
    }
}

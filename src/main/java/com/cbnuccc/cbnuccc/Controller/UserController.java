package com.cbnuccc.cbnuccc.Controller;

// import java.rmi.server.Operation;
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
import org.springframework.web.bind.annotation.ModelAttribute;
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

// UserController.java 상단 import 구문 모음
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;

@Tag(name="User Controller", description = "유저 정보 및 생성/수정, 프로필 설정 등의 기능")
@RestController
public class UserController {
    @Autowired
    private UserService userService;

    // 보안 강화를 위해 비밀번호 관련 기능을 일시적으로 제한함 (https://github.com/CBNU-CCC/cbnuccc-backend/issues/11)
    private static final boolean PASSWORD_FEATURE_ENABLED = false;

    @Operation(summary="메인 페이지", description="접속하면 보여지는 메인 페이지입니다.") 
    @GetMapping("/")
    public String home() {
        return "Hello!\nI am okay!\nYou found this... r u a programmer? haha.";
    }

    @Operation(summary = "모든 유저 정보", description = "모든 유저 정보-제한됨- 가져오기")
    @GetMapping("/user")
    public ResponseEntity<Object> getUser(
            @Parameter(hidden = true) Authentication authentication,
            @ModelAttribute LimitedUserDto userDto, Pageable pageable) {
        if (userDto == null)
            userDto = new LimitedUserDto();

        UUID callerUuid = userService.getUuidFromAuth(authentication);
        Page<LimitedUserDto> dtos = userService.findAllLimitedUserDtosByLimitedUserDto(userDto, pageable, callerUuid);
        LogUtil.printBasicInfoLog(LogHeader.GET_USER,
                LogUtil.makeCountKV(dtos.getNumberOfElements()),
                LogUtil.makePageNumberKV(pageable),
                LogUtil.makePageSizeKV(pageable));
        return ResponseEntity.ok(PaginationUtil.makePaginationMap(dtos));
    }

    @Operation(summary="특정 유저 정보", description="특정 사용자 정보-제한됨-만 가져오기")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "해당 UUID의 사용자를 찾을 수 없음", content = @Content)
    })
    @GetMapping("/user/{uuid}")
    public ResponseEntity<?> getUserByUuid(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "조회할 유저의 UUID") @PathVariable("uuid") UUID uuid) {
        LimitedUserDto user = new LimitedUserDto();
        user.setUuid(uuid);

        UUID callerUuid = userService.getUuidFromAuth(authentication);
        Page<LimitedUserDto> resultBody = userService.findAllLimitedUserDtosByLimitedUserDto(user,
                Pageable.ofSize(1), callerUuid);
        if (resultBody.getSize() == 0) {
            LogUtil.printBasicWarnLog(LogHeader.GET_USER, LogUtil.makeStatusCodeMessageKV(StatusCode.NO_USER_FOUND));
            return StatusCode.NO_USER_FOUND.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.GET_USER, LogUtil.makeCountKV(resultBody.getNumberOfElements()));
        LimitedUserDto result = resultBody.toList().get(0);
        return ResponseEntity.ok(result);
    }

    @Operation(summary="내 정보", description="내 정보 가져오기")
    @GetMapping("/me")
    public ResponseEntity<?> getMyUserData(@Parameter(hidden = true) Authentication authentication) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        Optional<UserDto> _me = userService.findUserDtoByUuid(uuid, uuid);
        if (_me.isEmpty()) {
            LogUtil.printBasicWarnLog(LogHeader.GET_ME, LogUtil.makeStatusCodeMessageKV(StatusCode.NO_USER_FOUND));
            return StatusCode.NO_USER_FOUND.makeErrorResponseEntity();
        }
        UserDto me = _me.get();

        LogUtil.printBasicInfoLog(LogHeader.GET_ME, (Object[]) null);
        return ResponseEntity.ok(me);
    }

    @Operation(summary="중복 이메일 확인", description="이메일 중복 확인하기")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "사용 가능한 이메일"),
        @ApiResponse(responseCode = "409", description = "이미 존재하는 이메일", content = @Content)
    })
    @GetMapping("/email-duplication")
    public ResponseEntity<?> checkEmailDuplication(@RequestBody Map<String, String> body) {
        if (!body.containsKey("email")) {
            LogUtil.printBasicWarnLog(LogHeader.CHECK_EMAIL_DUPLICATION,
                    LogUtil.makeStatusCodeMessageKV(StatusCode.NO_ENOUGH_ARGS));
            return StatusCode.NO_ENOUGH_ARGS.makeErrorResponseEntity();
        }
        String email = body.get("email").toLowerCase();

        // 회원가입 전 단계(비로그인)에서 호출되므로 본인(caller) 개념이 없음
        Optional<UserDto> _user = userService.findUserDtoByEmail(email, null);
        if (_user.isPresent()) {
            LogUtil.printBasicWarnLog(LogHeader.CHECK_EMAIL_DUPLICATION, LogUtil.makeStatusCodeMessageKV(
                    StatusCode.DUPLICATED_EMAIL));
            return StatusCode.DUPLICATED_EMAIL.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.CHECK_EMAIL_DUPLICATION, LogUtil.makeEmailKV(email));
        return StatusCode.NOT_DUPLICATED_EMAIL.makeErrorResponseEntity();
    }

    @Operation(summary="사용자 생성", description="사용자 생성하기 (이메일 중복 X). 소속 점검순은 affiliatedReviewSoon.id로 지정한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "사용자 생성 성공",
                content = @Content(examples = @ExampleObject(
                    name = "사용자 생성 응답 예시",
                    value = "{\"uuid\":\"27ad10b7-3a0f-432a-98e7-6c0290992a4c\",\"rank\":0,\"name\":\"박순원\",\"grade\":1,\"prayerCount\":0,\"missionCount\":0,\"affiliatedReviewSoon\":{\"id\":3,\"name\":\"3점검순\",\"isRepresentative\":false}}"
                ))),
        @ApiResponse(responseCode = "400", description = "비밀번호 형식이 유효하지 않음",
                content = @Content(examples = @ExampleObject(
                    name = "비밀번호 유효하지 않음 응답 예시",
                    value = "{\"errorCode\":18,\"message\":\"주어진 비밀번호가 유효하지 않습니다. (비밀번호는 8~15자리여야 하며, 하나 이상의 특수문자와 숫자가 들어가야 합니다.)\"}"
                ))),
        @ApiResponse(responseCode = "403", description = "이메일 인증이 완료되지 않음",
                content = @Content(examples = @ExampleObject(
                    name = "이메일 미인증 응답 예시",
                    value = "{\"errorCode\":10,\"message\":\"주어진 유저는 유효하지 않습니다. (이메일 인증이 안 되었습니다)\"}"
                ))),
        @ApiResponse(responseCode = "404", description = "지정한 소속 점검순(affiliatedReviewSoon.id)이 존재하지 않음",
                content = @Content(examples = @ExampleObject(
                    name = "점검순 없음 응답 예시",
                    value = "{\"errorCode\":25,\"message\":\"주어진 사용자가 소속된 점검순이 존재하지 않습니다.\"}"
                ))),
        @ApiResponse(responseCode = "409", description = "이미 존재하는 이메일",
                content = @Content(examples = @ExampleObject(
                    name = "이메일 중복 응답 예시",
                    value = "{\"errorCode\":1,\"message\":\"주어진 이메일이 중복됩니다.\"}"
                )))
    })
    @PostMapping("/user")
    public ResponseEntity<?> createUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "생성할 사용자 정보. affiliatedReviewSoon은 소속시킬 점검순이 있을 때만 보내면 된다.",
                required = true,
                content = @Content(examples = @ExampleObject(
                    name = "사용자 생성 요청 예시",
                    value = "{\"email\":\"example123@gmail.com\",\"password\":\"abc12345!\",\"studentId\":\"202012345\",\"rank\":0,\"sex\":true,\"name\":\"박순원\",\"grade\":1,\"affiliatedReviewSoon\":{\"id\":3}}"
                )))
            @RequestBody MyUser user) {
        DataWithStatusCode<LimitedUserDto> result = userService.createUser(user);
        StatusCode code = result.code();
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.CREATE_USER, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.CREATE_USER, LogUtil.makeUuidStringKV(result.data().getUuid()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result.data());
    }

    @Operation(summary="정보 수정", description="사용자 정보(본인) 수정하기. 필드를 보내지 않으면(null) 해당 항목은 변경되지 않는다. "
            + "소속 점검순은 affiliatedReviewSoon.id로 지정하며, affiliatedReviewSoon: {}(빈 객체)를 보내면 소속이 해제된다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "정보 수정 성공",
                content = @Content(examples = @ExampleObject(
                    name = "정보 수정 응답 예시",
                    value = "{\"uuid\":\"27ad10b7-3a0f-432a-98e7-6c0290992a4c\",\"email\":\"example123@gmail.com\",\"rank\":0,\"sex\":true,\"name\":\"박순원\",\"grade\":1,\"prayerCount\":2,\"missionCount\":1,\"affiliatedReviewSoon\":{\"id\":3,\"name\":\"3점검순\",\"isRepresentative\":true}}"
                ))),
        @ApiResponse(responseCode = "401", description = "인증 실패 (토큰 미제공 또는 만료)", content = @Content),
        @ApiResponse(responseCode = "403", description = "id/uuid/studentId/password 등 중요 정보를 수정하려고 함",
                content = @Content(examples = @ExampleObject(
                    name = "중요 정보 수정 시도 응답 예시",
                    value = "{\"errorCode\":3,\"message\":\"사용자의 중요 정보는 수정할 수 없습니다.\"}"
                ))),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없거나, 지정한 소속 점검순(affiliatedReviewSoon.id)이 존재하지 않음",
                content = @Content(examples = @ExampleObject(
                    name = "점검순 없음 응답 예시",
                    value = "{\"errorCode\":25,\"message\":\"주어진 사용자가 소속된 점검순이 존재하지 않습니다.\"}"
                )))
    })
    @PatchMapping("/user")
    public ResponseEntity<?> updateUser(
            @Parameter(hidden = true) Authentication authentication,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "수정할 사용자 정보. 변경하지 않을 필드는 생략(null)한다.",
                required = true,
                content = @Content(examples = @ExampleObject(
                    name = "소속 점검순만 변경하는 요청 예시",
                    value = "{\"affiliatedReviewSoon\":{\"id\":3}}"
                )))
            @RequestBody MyUser user) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        StatusCode code = userService.updateUserByUuid(uuid, user);
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.UPDATE_USER, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.UPDATE_USER, LogUtil.makeUuidStringKV(uuid));
        return getMyUserData(authentication);
    }

    @Operation(summary="비밀번호 수정", description="사용자 비밀번호 수정하기")
    @PatchMapping("/user/password")
    public ResponseEntity<?> updateUserPassword(@Parameter(hidden = true) Authentication authentication,
            @RequestBody OldAndNewPasswordDto passwords) {
        if (!PASSWORD_FEATURE_ENABLED) {
            LogUtil.printBasicWarnLog(LogHeader.UPDATE_USER_PASSWORD,
                    LogUtil.makeStatusCodeMessageKV(StatusCode.PASSWORD_FEATURE_TEMPORARILY_DISABLED));
            return StatusCode.PASSWORD_FEATURE_TEMPORARILY_DISABLED.makeErrorResponseEntity();
        }

        UUID uuid = userService.getUuidFromAuth(authentication);
        StatusCode code = userService.updateUserPasswordByUuid(uuid, passwords);
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.UPDATE_USER_PASSWORD, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.UPDATE_USER_PASSWORD, LogUtil.makeUuidStringKV(uuid));
        return getMyUserData(authentication);
    }

    @Operation(summary="비밀번호 초기화", description="비밀번호 초기화하기")
    @PatchMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordDto resetPasswordDto) {
        if (!PASSWORD_FEATURE_ENABLED) {
            LogUtil.printBasicWarnLog(LogHeader.RESET_PASSWORD,
                    LogUtil.makeStatusCodeMessageKV(StatusCode.PASSWORD_FEATURE_TEMPORARILY_DISABLED));
            return StatusCode.PASSWORD_FEATURE_TEMPORARILY_DISABLED.makeErrorResponseEntity();
        }

        StatusCode code = userService.resetPassword(resetPasswordDto);
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.RESET_PASSWORD, LogUtil.makeEmailKV(resetPasswordDto.getEmail()));
            return code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.RESET_PASSWORD, LogUtil.makeEmailKV(resetPasswordDto.getEmail()));
        return code.makeErrorResponseEntity();
    }

    @Operation(summary="사용자 삭제", description="사용자 삭제하기")
    @DeleteMapping("/user")
    public ResponseEntity<?> deleteUser(@Parameter(hidden = true) Authentication authentication) {
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

    @Operation(summary="프로필 업로드", description="사용자 프로필 사진 업로드하기")
    @PostMapping("/profile-image")
    public ResponseEntity<?> uploadProfileImage(@Parameter(hidden = true) Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        Optional<UserDto> _user = userService.findUserDtoByUuid(uuid, uuid);

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

    @Operation(summary="프로필 사진 삭제", description="사용자 프로필 사진 삭제하기")
    @DeleteMapping("/profile-image")
    public ResponseEntity<?> deleteProfileImage(@Parameter(hidden = true) Authentication authentication) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        Optional<UserDto> _user = userService.findUserDtoByUuid(uuid, uuid);
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

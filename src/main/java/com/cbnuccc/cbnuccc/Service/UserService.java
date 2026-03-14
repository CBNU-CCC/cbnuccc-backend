package com.cbnuccc.cbnuccc.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import com.cbnuccc.cbnuccc.Config.MailgunProperties;
import com.cbnuccc.cbnuccc.Config.SecurityConfig;
import com.cbnuccc.cbnuccc.Config.SupabaseProperties;
import com.cbnuccc.cbnuccc.Dto.LimitedUserDto;
import com.cbnuccc.cbnuccc.Dto.OldAndNewPasswordDto;
import com.cbnuccc.cbnuccc.Dto.ResetPasswordDto;
import com.cbnuccc.cbnuccc.Dto.UserDto;
import com.cbnuccc.cbnuccc.Model.MyUser;
import com.cbnuccc.cbnuccc.Model.Verification;
import com.cbnuccc.cbnuccc.Repository.MissionJpaRepository;
import com.cbnuccc.cbnuccc.Repository.PrayerJpaRepository;
import com.cbnuccc.cbnuccc.Repository.UserJpaRepository;
import com.cbnuccc.cbnuccc.Repository.VerificationJpaRepository;
import com.cbnuccc.cbnuccc.Util.DataWithStatusCode;
import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.OffsetDateTimeUtil;
import com.cbnuccc.cbnuccc.Util.SecurityUtil;
import com.cbnuccc.cbnuccc.Util.StatusCode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserJpaRepository userJpaRepository;
    private final VerificationJpaRepository verificationJpaRepository;
    private final PrayerJpaRepository prayerJpaRepository;
    private final MissionJpaRepository missionJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtil securityUtil;
    private final WebClient webClient;
    private final SupabaseProperties supabaseProperties;
    private final SecurityConfig securityConfig;
    private final MailgunProperties mailgunProperties;

    // make User to UserDto.
    private UserDto userToUserDto(MyUser user) {
        return new UserDto(
                user.getUuid(),
                user.getEmail(),
                user.getRank(),
                user.getSex(),
                user.getName(),
                user.getGrade(),
                prayerJpaRepository.countByAuthorUuid(user.getUuid()),
                missionJpaRepository.countByAuthorUuid(user.getUuid()));
    }

    // make UserDto to User.
    private MyUser userDtoToUser(UserDto userDto) {
        MyUser user = new MyUser();
        user.setEmail(userDto.getEmail());
        user.setUuid(userDto.getUuid());
        user.setRank(userDto.getRank());
        user.setSex(userDto.getSex());
        user.setName(userDto.getName());
        user.setGrade(userDto.getGrade());
        return user;
    }

    // make UserDto to LimitedUserDto.
    private LimitedUserDto userDtoToLimitedUserDto(UserDto userDto) {
        LimitedUserDto dto = new LimitedUserDto(
                userDto.getUuid(),
                userDto.getRank(),
                userDto.getName(),
                userDto.getGrade(),
                userDto.getPrayerCount(),
                userDto.getMissionCount());
        return dto;
    }

    // make LimitedUserDto to UserDto.
    private UserDto limitedUserDtoToUserDto(LimitedUserDto limitedUserDto) {
        UserDto dto = new UserDto();
        dto.setUuid(limitedUserDto.getUuid());
        dto.setRank(limitedUserDto.getRank());
        dto.setName(limitedUserDto.getName());
        dto.setGrade(limitedUserDto.getGrade());
        return dto;
    }

    // make user's password encoded.
    private MyUser encodeUserPassword(MyUser user, String planePassword) {
        String encodedPassword = passwordEncoder.encode(securityUtil.addPepper(planePassword));
        user.setPassword(encodedPassword);
        return user;
    }

    // check a user by email if it is duplicated.
    private boolean checkDuplicatedUserByEmail(String email) {
        Optional<MyUser> user = userJpaRepository.findByEmail(email);
        return user.isPresent();
    }

    // if the email is verified, it returns true.
    // otherwise, it returns false.
    // also, if the email is not on the DB, it returns false.
    private boolean checkIsVerifiedEmail(String email) {
        Optional<Verification> _verification = verificationJpaRepository.findByEmail(email);
        if (_verification.isEmpty())
            return false;
        Verification verification = _verification.get();
        return verification.getIsVerified();
    }

    // find UserDto by given uuid.
    public Optional<UserDto> findUserDtoByUuid(UUID uuid) {
        Optional<MyUser> _user = userJpaRepository.findByUuid(uuid);
        if (_user.isEmpty())
            return Optional.ofNullable(null);
        UserDto result = userToUserDto(_user.get());
        return Optional.of(result);
    }

    // find UserDto by given email.
    public Optional<UserDto> findUserDtoByEmail(String email) {
        Optional<MyUser> _user = userJpaRepository.findByEmail(email);
        if (_user.isEmpty())
            return Optional.ofNullable(null);
        UserDto result = userToUserDto(_user.get());
        return Optional.of(result);
    }

    // find LimitedUserDto by given uuid.
    public Optional<LimitedUserDto> findLimitedUserDtoByUuid(UUID uuid) {
        Optional<MyUser> _user = userJpaRepository.findByUuid(uuid);
        if (_user.isEmpty())
            return Optional.ofNullable(null);
        LimitedUserDto result = userDtoToLimitedUserDto(userToUserDto(_user.get()));
        return Optional.of(result);
    }

    // find all of users that are matched with given UserDto.
    public Page<LimitedUserDto> findAllLimitedUserDtosByLimitedUserDto(LimitedUserDto exampleUser, Pageable pageable) {
        // make LimitedUserDto to User
        MyUser example = userDtoToUser(limitedUserDtoToUserDto(exampleUser));
        Page<MyUser> users = userJpaRepository.findAll(Example.of(example), pageable);

        return users.map(user -> userDtoToLimitedUserDto(userToUserDto(user)));
    }

    // get uuid from given jwt token.
    public UUID getUuidFromAuth(Authentication authentication) {
        String uuidString = (String) authentication.getPrincipal();
        UUID uuid = UUID.fromString(uuidString);
        return uuid;
    }

    // create a user.
    public DataWithStatusCode<LimitedUserDto> createUser(MyUser user) {
        user.setUuid(UUID.randomUUID());
        String email = user.getEmail();

        if (checkDuplicatedUserByEmail(email))
            return new DataWithStatusCode<>(StatusCode.DUPLICATED_EMAIL, null);

        if (!checkIsVerifiedEmail(email))
            return new DataWithStatusCode<>(StatusCode.NOT_VERIFIED, null);

        // checking and encoding the password.
        if (!securityUtil.checkValidPassword(user.getPassword()))
            return new DataWithStatusCode<>(StatusCode.INVALID_PASSWORD, null);
        user = encodeUserPassword(user, user.getPassword());

        try {
            MyUser createdUser = userJpaRepository.save(user);
            verificationJpaRepository.deleteByEmail(email); // delete verified user from verification table.
            LimitedUserDto createdLimitedUserDto = userDtoToLimitedUserDto(userToUserDto(createdUser));
            return new DataWithStatusCode<LimitedUserDto>(StatusCode.NO_ERROR, createdLimitedUserDto);
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.CREATE_USER, LogUtil.makeExceptionKV(e));
            return new DataWithStatusCode<>(StatusCode.SOMETHING_WENT_WRONG, null);
        }
    }

    // update a user to given user by uuid.
    // if any given user's field is null,
    // the matched field of the user is not changed.
    public StatusCode updateUserByUuid(UUID uuid, MyUser user) {
        Optional<MyUser> _oldUser = userJpaRepository.findByUuid(uuid);
        if (_oldUser.isEmpty())
            return StatusCode.NO_USER_FOUND;

        MyUser oldUser = _oldUser.get();
        if (user.getId() != null ||
                user.getUuid() != null ||
                user.getStudentId() != null ||
                user.getPassword() != null)
            return StatusCode.CONNOT_CHANGE_IMPORTANT_INFORMATION;

        // if the field value is not null, change it.
        if (user.getEmail() != null)
            oldUser.setEmail(user.getEmail());
        if (user.getRank() != null)
            oldUser.setRank(user.getRank());
        if (user.getSex() != null)
            oldUser.setSex(user.getSex());
        if (user.getName() != null)
            oldUser.setName(user.getName());
        if (user.getGrade() != null)
            oldUser.setGrade(user.getGrade());

        userJpaRepository.save(oldUser);
        return StatusCode.NO_ERROR;
    }

    // update user's password by uuid.
    public StatusCode updateUserPasswordByUuid(UUID uuid, OldAndNewPasswordDto passwords) {
        Optional<MyUser> _user = userJpaRepository.findByUuid(uuid);
        if (_user.isEmpty())
            return StatusCode.NO_USER_FOUND;
        MyUser user = _user.get();

        // only can change password 5 minutes after the last time to change it.
        if (user.getPasswordChangedAt().isAfter(OffsetDateTimeUtil.getNow().minusMinutes(5)))
            return StatusCode.CANNOT_CHANGE_PASSWORD_WITHIN_5_MINUTES;

        // check old password is matched
        String oldPassword = securityUtil.addPepper(passwords.getOldPassword());
        boolean isMatchedPassword = securityConfig.passwordEncoder().matches(oldPassword, user.getPassword());
        if (!isMatchedPassword)
            return StatusCode.PASSWORD_IS_INCURRECT;

        // check if given new password is valid
        boolean isValidPassword = securityUtil.checkValidPassword(passwords.getNewPassword());
        if (!isValidPassword)
            return StatusCode.INVALID_PASSWORD;

        // change the password
        user.setPasswordChangedAt(OffsetDateTimeUtil.getNow());
        user = encodeUserPassword(user, passwords.getNewPassword());
        userJpaRepository.save(user);
        return StatusCode.NO_ERROR;
    }

    // send a email to reset password
    public StatusCode resetPassword(ResetPasswordDto resetPasswordDto) {
        // make example
        MyUser example = new MyUser();
        example.setEmail(resetPasswordDto.getEmail());
        example.setName(resetPasswordDto.getName());
        example.setStudentId(resetPasswordDto.getStudentId());

        // find matched user
        List<MyUser> _user = userJpaRepository.findAll(Example.of(example));
        if (_user.isEmpty() || _user.size() > 1)
            return StatusCode.NO_USER_FOUND;
        MyUser user = _user.get(0);

        // only can change password 5 minutes after the last time to change it.
        if (user.getPasswordChangedAt().isAfter(OffsetDateTimeUtil.getNow().minusMinutes(5)))
            return StatusCode.CANNOT_CHANGE_PASSWORD_WITHIN_5_MINUTES;

        // make new password
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String specials = "!@#$%^&*()-_=+[]{};:,.<>?";

        StringBuilder password = new StringBuilder();

        // Add 4 random alphabets (upper/lower)
        for (int i = 0; i < 4; i++) {
            char c = (Math.random() < 0.5 ? upper : lower).charAt((int) (Math.random() * 26));
            password.append(c);
        }

        // Add 5 random digits
        for (int i = 0; i < 5; i++) {
            password.append(digits.charAt((int) (Math.random() * digits.length())));
        }

        // Add 2 random special characters
        for (int i = 0; i < 2; i++) {
            password.append(specials.charAt((int) (Math.random() * specials.length())));
        }

        // Shuffle the password
        char[] passwordChars = password.toString().toCharArray();
        for (int i = passwordChars.length - 1; i > 0; i--) {
            int j = (int) (Math.random() * (i + 1));
            char temp = passwordChars[i];
            passwordChars[i] = passwordChars[j];
            passwordChars[j] = temp;
        }
        String newPassword = new String(passwordChars);

        // Encode and update password
        user.setPasswordChangedAt(OffsetDateTimeUtil.getNow());
        user = encodeUserPassword(user, newPassword);
        userJpaRepository.save(user);

        // send email to report it.
        String messageHeader = "안녕하세요!\n충북대학교 CCC입니다.\n아래와 같이 비밀번호가 초기화되었음을 알려드립니다.";
        String messageCode = "새 비밀번호: " + newPassword;
        String messageFooter = "위 비밀번호를 아무에게도 공개하지 마세요!\n로그인하신 후 즉시 비밀번호를 변경해주세요.\n감사합니다.";
        final String apiKey = mailgunProperties.getKey();
        final String senderDomain = mailgunProperties.getDomain();
        try {
            HttpResponse<JsonNode> request = Unirest
                    .post("https://api.mailgun.net/v3/" + senderDomain + "/messages")
                    .basicAuth("api", apiKey)
                    .queryString("from", "CBNU CCC <postmaster@" + senderDomain + ">")
                    .queryString("to", user.getEmail())
                    .queryString("subject", "[CBNU CCC] 🌱 비밀번호가 초기화되었습니다.")
                    .queryString("text",
                            messageHeader + "\n\n" + messageCode + "\n\n" + messageFooter)
                    .asJson();

            // that the status code is 200 means doing right operation.
            if (request.getStatus() != 200)
                return StatusCode.SOMETHING_WENT_WRONG;
        } catch (UnirestException e) {
            LogUtil.printBasicWarnLog(LogHeader.SEND_REGISTRATION_EMAIL, e.getMessage(), null);
            return StatusCode.SOMETHING_WENT_WRONG;
        }

        return StatusCode.NO_ERROR;
    }

    // delete a user by uuid.
    public StatusCode deleteUserByUuid(UUID uuid) {
        Optional<MyUser> _user = userJpaRepository.findByUuid(uuid);
        if (_user.isEmpty())
            return StatusCode.NO_USER_FOUND;

        MyUser user = _user.get();
        try {
            userJpaRepository.delete(user);
            return StatusCode.NO_ERROR;
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.DELETE_USER, LogUtil.makeExceptionKV(e));
            return StatusCode.SOMETHING_WENT_WRONG;
        }
    }

    // upload user profile image by uuid.
    public StatusCode uploadProfileImage(MultipartFile file, UUID uuid) {
        try {
            // extract extension
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains("."))
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));

            // set file name
            String fileName = uuid.toString();
            String path = "profile/" + fileName;

            // upload
            webClient.post()
                    .uri(supabaseProperties.getUrl() + "/storage/v1/object/" + path)
                    .header("Authorization", "Bearer " + supabaseProperties.getKey())
                    .header("apikey", supabaseProperties.getKey())
                    .header("Content-Type", "image/" + extension)
                    .header("x-upsert", "true")
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .bodyValue(file.getBytes())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return StatusCode.NO_ERROR;
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.UPLOAD_PROFILE_IMAGE, LogUtil.makeExceptionKV(e));
            return StatusCode.SOMETHING_WENT_WRONG;
        }
    }

    // delete user profile image by uuid.
    public StatusCode deleteProfileImage(UUID uuid) {
        try {
            // set file name
            String fileName = uuid.toString();
            String path = "profile/" + fileName;

            // delete
            webClient.delete()
                    .uri(supabaseProperties.getUrl() + "/storage/v1/object/" + path)
                    .header("Authorization", "Bearer " + supabaseProperties.getKey())
                    .header("apikey", supabaseProperties.getKey())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return StatusCode.NO_ERROR;
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.DELETE_PROFILE_IMAGE, LogUtil.makeExceptionKV(e));
            return StatusCode.SOMETHING_WENT_WRONG;
        }
    }
}

package com.cbnuccc.cbnuccc.Service;

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

import com.cbnuccc.cbnuccc.Config.SupabaseProperties;
import com.cbnuccc.cbnuccc.Dto.LimitedUserDto;
import com.cbnuccc.cbnuccc.Dto.UserDto;
import com.cbnuccc.cbnuccc.Model.MyUser;
import com.cbnuccc.cbnuccc.Model.Verification;
import com.cbnuccc.cbnuccc.Repository.PrayerJpaRepository;
import com.cbnuccc.cbnuccc.Repository.UserJpaRepository;
import com.cbnuccc.cbnuccc.Repository.VerificationJpaRepository;
import com.cbnuccc.cbnuccc.Util.DataWithStatusCode;
import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.SecurityUtil;
import com.cbnuccc.cbnuccc.Util.StatusCode;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserJpaRepository userJpaRepository;
    private final VerificationJpaRepository verificationJpaRepository;
    private final PrayerJpaRepository prayerJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtil securityUtil;
    private final WebClient webClient;
    private final SupabaseProperties supabaseProperties;

    // make User to UserDto.
    private UserDto userToUserDto(MyUser user) {
        return new UserDto(
                user.getUuid(),
                user.getEmail(),
                user.getRank(),
                user.getSex(),
                user.getName(),
                user.getGrade(),
                prayerJpaRepository.countByAuthorUuid(user.getUuid()));
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
                userDto.getPrayerCount());
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
    @Transactional
    public DataWithStatusCode<LimitedUserDto> createUser(MyUser user) {
        user.setUuid(UUID.randomUUID());
        String email = user.getEmail();

        if (checkDuplicatedUserByEmail(email))
            return new DataWithStatusCode<>(StatusCode.DUPLICATED_EMAIL, null);

        if (!checkIsVerifiedEmail(email))
            return new DataWithStatusCode<>(StatusCode.NOT_VERIFIED, null);

        // encoding the password.
        user = encodeUserPassword(user, user.getPassword());

        try {
            MyUser createdUser = userJpaRepository.save(user);
            verificationJpaRepository.deleteByEmail(email); // delete verified user from verification table.
            LimitedUserDto createdLimitedUserDto = userDtoToLimitedUserDto(userToUserDto(createdUser));
            return new DataWithStatusCode<LimitedUserDto>(StatusCode.NO_ERROR, createdLimitedUserDto);
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.CREATE_USER, e.getMessage(), null);
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
                user.getStudentId() != null)
            return StatusCode.CONNOT_CHANGE_IMPORTANT_INFORMATION;

        // if the field value is not null, change it.
        if (user.getEmail() != null)
            oldUser.setEmail(user.getEmail());
        if (user.getPassword() != null) {
            oldUser = encodeUserPassword(oldUser, user.getPassword());
        }
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
            LogUtil.printBasicWarnLog(LogHeader.DELETE_USER, e.getMessage(), uuid);
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
            LogUtil.printBasicWarnLog(LogHeader.UPLOAD_PROFILE_IMAGE, e.getMessage(), uuid);
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
            LogUtil.printBasicWarnLog(LogHeader.DELETE_PROFILE_IMAGE, e.getMessage(), uuid);
            return StatusCode.SOMETHING_WENT_WRONG;
        }
    }
}

package com.cbnuccc.cbnuccc.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.cbnuccc.cbnuccc.ErrorCode;
import com.cbnuccc.cbnuccc.SecurityUtil;
import com.cbnuccc.cbnuccc.Dto.UserDto;
import com.cbnuccc.cbnuccc.Model.User;
import com.cbnuccc.cbnuccc.Repository.UserJpaRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UserService {
    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private final PasswordEncoder passwordEncoder;

    @Autowired
    private final SecurityUtil securityUtil;

    // make User to UserDto.
    private UserDto UserToUserDto(User user) {
        return new UserDto(
                user.getUuid(),
                user.getEmail(),
                user.getRank(),
                user.getSex(),
                user.getName(),
                user.getGrade(),
                user.getBirthDate());
    }

    // make UserDto to User.
    private User UserDtoToUser(UserDto userDto) {
        User user = new User();
        user.setEmail(userDto.getEmail());
        user.setUuid(userDto.getUuid());
        user.setRank(userDto.getRank());
        user.setSex(userDto.getSex());
        user.setName(userDto.getName());
        user.setGrade(userDto.getGrade());
        user.setBirthDate(userDto.getBirthDate());
        return user;
    }

    // make encoded string from given string.
    private String encodePassword(String password) {
        return passwordEncoder.encode(securityUtil.addPepper(password));
    }

    // check if plane and hashed string are actually same.
    private boolean checkMatched(String planePassword, String encodedPassword) {
        return passwordEncoder.matches(securityUtil.addPepper(planePassword), encodedPassword);
    }

    // make user's password encoded.
    private User makeUserPasswordEncoded(User user) {
        String planePassword = user.getPassword();
        String encodedPassword = encodePassword(planePassword);
        user.setPassword(encodedPassword);
        return user;
    }

    // find a user by given uuid.
    public Optional<UserDto> findUserByUuid(UUID uuid) {
        Optional<User> _user = userJpaRepository.findByUuid(uuid);
        if (_user.isEmpty())
            return Optional.ofNullable(null);
        UserDto result = UserToUserDto(_user.get());
        return Optional.of(result);
    }

    // find all of users that are matched with given UserDto.
    public List<UserDto> findAllMatchedUsers(UserDto exampleUser) {
        User example = UserDtoToUser(exampleUser);
        List<User> users = userJpaRepository.findAll(Example.of(example));

        List<UserDto> ret = new ArrayList<UserDto>();
        for (User user : users) {
            UserDto userDto = UserToUserDto(user);
            ret.add(userDto);
        }

        return ret;
    }

    // create a user.
    public ResponseEntity<?> createUser(User user) {
        user.setUuid(UUID.randomUUID());

        if (checkDuplicatedUserByEmail(user.getEmail()))
            return ErrorCode.DUPLICATED_EMAIL.makeErrorResponseEntity();

        // encoding the password.
        user = makeUserPasswordEncoded(user);

        try {
            User createdUser = userJpaRepository.save(user);
            UserDto createdUserDto = UserToUserDto(createdUser);
            return ResponseEntity.status(HttpStatus.OK).body(createdUserDto);
        } catch (Exception e) {
            System.err.println(e);
            return ErrorCode.SOMETHING_WENT_WRONG.makeErrorResponseEntity();
        }
    }

    // check a user by email if it is duplicated.
    public boolean checkDuplicatedUserByEmail(String email) {
        Optional<User> user = userJpaRepository.findByEmail(email);
        return user.isPresent();
    }

    // update a user to given user by uuid.
    // if any given user's field is null,
    // the matched field of the user is not changed.
    public ErrorCode updateUserByUuid(UUID uuid, User user) {
        Optional<User> _oldUser = userJpaRepository.findByUuid(uuid);
        if (_oldUser.isEmpty())
            return ErrorCode.NO_USER_FOUND;

        User oldUser = _oldUser.get();
        if (user.getId() != null ||
                user.getUuid() != null ||
                user.getStudentId() != null)
            return ErrorCode.CONNOT_CHANGE_IMPORTANT_INFORMATION;

        // if the field value is not null, change it.
        if (user.getEmail() != null)
            oldUser.setEmail(user.getEmail());
        if (user.getPassword() != null) {
            oldUser = makeUserPasswordEncoded(oldUser);
        }
        if (user.getRank() != null)
            oldUser.setRank(user.getRank());
        if (user.getSex() != null)
            oldUser.setSex(user.getSex());
        if (user.getName() != null)
            oldUser.setName(user.getName());
        if (user.getGrade() != null)
            oldUser.setGrade(user.getGrade());
        if (user.getBirthDate() != null)
            oldUser.setBirthDate(user.getBirthDate());

        userJpaRepository.save(oldUser);
        return ErrorCode.NO_ERROR;
    }

    // delete a user by uuid.
    public ErrorCode deleteUserByUuid(UUID uuid) {
        Optional<User> _user = userJpaRepository.findByUuid(uuid);
        if (_user.isEmpty())
            return ErrorCode.NO_USER_FOUND;

        User user = _user.get();
        userJpaRepository.delete(user);
        return ErrorCode.NO_ERROR;
    }
}

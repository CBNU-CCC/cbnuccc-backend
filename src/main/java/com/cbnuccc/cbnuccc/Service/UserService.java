package com.cbnuccc.cbnuccc.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import com.cbnuccc.cbnuccc.ErrorCode;
import com.cbnuccc.cbnuccc.Dto.UserDto;
import com.cbnuccc.cbnuccc.Model.User;
import com.cbnuccc.cbnuccc.Repository.UserJpaRepository;

@Service
public class UserService {
    @Autowired
    private UserJpaRepository userJpaRepository;

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

    public Optional<UserDto> findUserByUuid(UUID uuid) {
        Optional<User> _user = userJpaRepository.findByUuid(uuid);
        if (_user.isEmpty())
            return Optional.ofNullable(null);
        UserDto result = UserToUserDto(_user.get());
        return Optional.of(result);
    }

    public List<UserDto> getAllUsers(UserDto exampleUser) {
        User example = UserDtoToUser(exampleUser);
        List<User> users = userJpaRepository.findAll(Example.of(example));

        List<UserDto> ret = new ArrayList<UserDto>();
        for (User user : users) {
            UserDto userDto = UserToUserDto(user);
            ret.add(userDto);
        }

        return ret;
    }

    public Optional<UserDto> createUser(User user) {
        user.setUuid(UUID.randomUUID());
        user.setSalt("testsalt");

        try {
            userJpaRepository.save(user);
            return Optional.ofNullable(UserToUserDto(user));
        } catch (Exception e) {
            return null;
        }
    }

    public boolean checkDuplicatedUserByEmail(String email) {
        Optional<User> user = userJpaRepository.findByEmail(email);
        return user.isPresent();
    }

    public ErrorCode updateUserByUuid(UUID uuid, User user) {
        Optional<User> _oldUser = userJpaRepository.findByUuid(uuid);
        if (_oldUser.isEmpty())
            return ErrorCode.NO_USER_FOUND;

        User oldUser = _oldUser.get();
        if (user.getId() != null ||
                user.getUuid() != null ||
                user.getSalt() != null ||
                user.getStudentId() != null)
            return ErrorCode.CONNOT_CHANGE_IMPORTANT_INFORMATION;

        if (user.getEmail() != null)
            oldUser.setEmail(user.getEmail());
        if (user.getPassword() != null)
            oldUser.setPassword(user.getPassword());
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

    public ErrorCode deleteUserByUuid(UUID uuid) {
        Optional<User> _user = userJpaRepository.findByUuid(uuid);
        if (_user.isEmpty())
            return ErrorCode.NO_USER_FOUND;

        User user = _user.get();
        userJpaRepository.delete(user);
        return ErrorCode.NO_ERROR;
    }
}

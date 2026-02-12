package com.cbnuccc.cbnuccc.Controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.cbnuccc.cbnuccc.ErrorCode;
import com.cbnuccc.cbnuccc.Dto.UserDto;
import com.cbnuccc.cbnuccc.Model.MyUser;
import com.cbnuccc.cbnuccc.Service.UserService;

@RestController
public class UserController {
    @Autowired
    private UserService userService;

    // main page
    @GetMapping("/")
    public String home() {
        return "Hello!";
    }

    // get users like userDto
    @GetMapping("/user")
    public ResponseEntity<List<UserDto>> getUser(
            @ModelAttribute UserDto userDto) {
        List<UserDto> dtos = userService.findAllMatchedUsers(userDto);
        return ResponseEntity.ok(dtos);
    }

    // get a user by uuid
    @GetMapping("/user/{uuid}")
    public ResponseEntity<?> getUserByUuid(@PathVariable("uuid") UUID uuid) {
        UserDto user = new UserDto();
        user.setUuid(uuid);

        List<UserDto> resultBody = (List<UserDto>) getUser(user).getBody();
        if (resultBody.size() == 0)
            return ErrorCode.NO_USER_FOUND.makeErrorResponseEntity();

        UserDto result = resultBody.get(0);
        return ResponseEntity.ok(result);
    }

    // create user, but the user's email should not be same with other's email.
    @PostMapping("/user")
    public ResponseEntity<?> createUser(@RequestBody MyUser user) {
        return userService.createUser(user);
    }

    // update user by uuid
    @PatchMapping("/user")
    public ResponseEntity<?> updateUser(Authentication authentication, @RequestBody MyUser user) {
        // process given jwt token.
        String uuidString = (String) authentication.getPrincipal();
        UUID uuid = UUID.fromString(uuidString);

        // update user.
        ErrorCode resultCode = userService.updateUserByUuid(uuid, user);
        if (resultCode != ErrorCode.NO_ERROR)
            return resultCode.makeErrorResponseEntity();
        return getUserByUuid(uuid);
    }

    // delete a user by uuid
    @DeleteMapping("/user")
    public ResponseEntity<?> deleteUser(Authentication authentication) {
        // process given jwt token.
        String uuidString = (String) authentication.getPrincipal();
        UUID uuid = UUID.fromString(uuidString);

        // delete user.
        Optional<UserDto> _deletedUser = userService.findUserByUuid(uuid);

        ErrorCode resultCode = userService.deleteUserByUuid(uuid);
        if (resultCode != ErrorCode.NO_ERROR || _deletedUser.isEmpty())
            return resultCode.makeErrorResponseEntity();

        UserDto deletedUser = _deletedUser.get();
        return ResponseEntity.ok(deletedUser);
    }
}

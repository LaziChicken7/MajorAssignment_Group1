package org.auctionfx.demodatabase.controller;

import org.auctionfx.demodatabase.dto.request.UserCreationRequest;
import org.auctionfx.demodatabase.dto.request.UserUpdateRequest;
import org.auctionfx.demodatabase.entity.User;
import org.auctionfx.demodatabase.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
// Về bản chất "/users" được dùng rất nhiều nên để tránh mỗi lần khai báo phải viết lại thì làm như này sẽ đỡ viết hơn
public class UserController {
    @Autowired
    private UserService userService;

    // Create
    @PostMapping
    User createUser(@RequestBody UserCreationRequest request) {
        return userService.createUser(request);
    }

    // Read
    @GetMapping
    List<User> getUsers() {
        return userService.getUsers();
    }

    @GetMapping("/{userId}")
    User getUser(@PathVariable("userId") String userId) {
        return userService.getUser(userId);
    }

    // Update
    @PutMapping("/{userId}")
    User updateUser(@PathVariable String userId, @RequestBody UserUpdateRequest request) {
        return userService.updateUser(userId, request);
    }

    // Delete
    @DeleteMapping("/{userId}")
    String deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return "User has been deleted";
    }
}

package org.auctionfx.auctionbidsystemspringbootrework.controller;

import jakarta.validation.Valid;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.*;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.auctionfx.auctionbidsystemspringbootrework.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserService userService;

    // Create
    @PostMapping("/register")
    ApiResponse<User> createUser(@RequestBody @Valid UserCreationRequest request) {
        ApiResponse<User> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.createUser(request));
        return apiResponse;
    }

    // Read
    @GetMapping("/admin")
    List<User> getUsers() { return userService.getUsers(); }

    @GetMapping("/admin/{userId}")
    User getUser(@PathVariable("userId") String userId) { return userService.getUser(userId); }

    // Update
    @PutMapping("/admin/{userId}")
    User updateUser(@PathVariable("userId") String userId, @RequestBody @Valid UserUpdateRequest request) {
        return userService.updateUser(userId, request);
    }

    // Delete
    @DeleteMapping("/admin/{userId}")
    String deleteUser(@PathVariable("userId") String userId) {
        return userService.deleteUser(userId);
    }

    // Upgrade to Seller
    @PutMapping("/upgrade-to-seller/{userName}")
    public String upgradeToSeller(@PathVariable String userName) {
        return userService.upgradeBidderToSeller(userName);
    }

    // Quên mật khẩu
    @PostMapping("/verify-reset-info")
    public ApiResponse<String> verifyUserInfo(@RequestBody VerifyInfoRequest request) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        // API này sẽ trả về cái Mã bí mật
        apiResponse.setResult(userService.verifyUserInfo(request));
        return apiResponse;
    }

    @PutMapping("/reset-password")
    public ApiResponse<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.resetPassword(request));
        return apiResponse;
    }
}

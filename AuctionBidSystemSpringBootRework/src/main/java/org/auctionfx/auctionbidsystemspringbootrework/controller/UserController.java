package org.auctionfx.auctionbidsystemspringbootrework.controller;

import jakarta.validation.Valid;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.*;
import org.auctionfx.auctionbidsystemspringbootrework.repository.*;
import org.auctionfx.auctionbidsystemspringbootrework.dto.response.ApiResponse;
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
    public ApiResponse<User> createUser(@RequestBody @Valid UserCreationRequest request) {
        ApiResponse<User> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.createUser(request));
        return apiResponse;
    }

    // Read - Danh sách User
    @GetMapping("/admin")
    public ApiResponse<List<User>> getUsers() {
        ApiResponse<List<User>> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.getUsers());
        return apiResponse;
    }

    // Read - Lấy 1 User
    @GetMapping("/admin/{userId}")
    public ApiResponse<User> getUser(@PathVariable("userId") String userId) {
        ApiResponse<User> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.getUser(userId));
        return apiResponse;
    }

    // Update
    @PutMapping("/admin/{userId}")
    public ApiResponse<User> updateUser(@PathVariable("userId") String userId, @RequestBody @Valid UserUpdateRequest request) {
        ApiResponse<User> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.updateUser(userId, request));
        return apiResponse;
    }

    // Cập nhật thông tin cá nhân (Dành cho User tự cập nhật)
    @PutMapping("/profile/{userName}")
    public ApiResponse<User> updateMyProfile(@PathVariable String userName, @RequestBody @Valid UserUpdateRequest request) {
        ApiResponse<User> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.updateMyProfile(userName, request));
        return apiResponse;
    }

    // Delete
    @DeleteMapping("/admin/{userId}")
    public ApiResponse<String> deleteUser(@PathVariable("userId") String userId) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.deleteUser(userId));
        return apiResponse;
    }

    // Upgrade to Seller
    @PutMapping("/upgrade-to-seller/{userName}")
    public ApiResponse<String> upgradeToSeller(@PathVariable String userName) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.upgradeBidderToSeller(userName));
        return apiResponse;
    }

    // Đăng nhập
    @PostMapping("/login")
    public ApiResponse<User> login(@RequestBody LoginRequest request) {
        ApiResponse<User> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.login(request));
        return apiResponse;
    }

    // Quên mật khẩu - Xác thực
    @PostMapping("/verify-reset-info")
    public ApiResponse<String> verifyUserInfo(@RequestBody VerifyInfoRequest request) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        // API này sẽ trả về cái Mã bí mật
        apiResponse.setResult(userService.verifyUserInfo(request));
        return apiResponse;
    }

    // Quên mật khẩu - Đổi pass
    @PutMapping("/reset-password")
    public ApiResponse<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.resetPassword(request));
        return apiResponse;
    }

    // Lấy thông tin cá nhân bằng userName
    @GetMapping("/profile/{userName}")
    public ApiResponse<User> getMyProfile(@PathVariable String userName) {
        ApiResponse<User> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.getUserByUserName(userName));
        return apiResponse;
    }
}
package org.auctionfx.auctionbidsystemspringbootrework.controller;

import jakarta.validation.Valid;

import org.apache.catalina.connector.Response;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.*;
import org.auctionfx.auctionbidsystemspringbootrework.repository.*;
import org.auctionfx.auctionbidsystemspringbootrework.dto.response.ApiResponse;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.auctionfx.auctionbidsystemspringbootrework.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

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
    @DeleteMapping("/admin/{userName}")
    public ApiResponse<String> deleteUser(@PathVariable("userName") String userName) {
        User user = userService.getUserByUserName(userName);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.deleteUser(user.getId()));
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

    // Nút Khóa / Mở khóa tài khoản
    @PutMapping("/admin/{userName}/ban")
    public ApiResponse<String> toggleBanUser(@PathVariable("userName") String userName) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.toggleBanUser(userName));
        return apiResponse;
    }

    // Upload avatar cho User
    @PostMapping("/{userName}/avatar")
    public ApiResponse<String> uploadAvatar(@PathVariable String userName, @RequestParam("file") MultipartFile file) throws IOException {
        ApiResponse<String> apiResponse = new ApiResponse<>();

        // Kiểm tra file 
        if (file == null || file.isEmpty()) {
            throw new IOException("No file uploaded or file is empty");
        }

        // Kiểm tra user có tồn tại trong database không
        User user = userService.getUserByUserName(userName);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // 1. ĐỔI ĐƯỜNG DẪN LƯU THÀNH THƯ MỤC AVATAR
        String uploadDir = "uploads/images/avatar/";
        File dir = new File(uploadDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create upload directory");
        }

        // Lấy phần mở rộng của file (ví dụ: .png, .jpg)
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        // 2. ĐẶT TÊN FILE THEO USERNAME KÈM ĐUÔI FILE (VD: nguoiban_01.png)
        String newFileName = userName + fileExtension;

        // Đường dẫn đầy đủ để lưu file
        Path filePath = Paths.get(uploadDir + newFileName);

        // Ghi đè file mới vào thư mục (nếu user up ảnh mới, ảnh cũ cùng tên sẽ bị ghi đè)
        Files.write(filePath, file.getBytes());

        // 3. TẠO URL TRUY CẬP ĐÚNG CHUẨN
        String fileUrl = "/uploads/images/avatar/" + newFileName;

        // Cập nhật avatarUrl trong database cho user này
        user.setAvatarUrl(fileUrl);
        userService.saveUser(user);

        // Trả về message thành công
        apiResponse.setResult("Avatar uploaded successfully: " + fileUrl);
        return apiResponse;
    }

    // Gỡ avatar ( đưa về avatar mặc định)
    @DeleteMapping("/{userName}/avatar")
    public ApiResponse<String> removeAvatar(@PathVariable String userName) {
        ApiResponse<String> apiResponse = new ApiResponse<>();

        // lấy user từ database
        User user = userService.getUserByUserName(userName);

        // LƯU Ý: Phải có dấu "/" ở đầu chuỗi để Frontend ghép link không bị lỗi
        user.setAvatarUrl("/uploads/images/avatar/avatarmacdinh.png");

        // Dùng hàm lưu nhanh user 
        userService.saveUser(user);

        apiResponse.setCode(1000);
        apiResponse.setMessage("deleted avatar successfully");
        apiResponse.setResult(user.getAvatarUrl());

        return apiResponse;
    }
}
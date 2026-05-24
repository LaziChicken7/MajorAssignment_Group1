package org.auctionfx.auctionbidsystemspringbootrework.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.*;
import org.auctionfx.auctionbidsystemspringbootrework.dto.response.ApiResponse;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.auctionfx.auctionbidsystemspringbootrework.service.SearchService;
import org.auctionfx.auctionbidsystemspringbootrework.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/users")
@Slf4j // Kích hoạt bộ ghi log của Lombok
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private SearchService searchService;

    // Create
    @PostMapping("/register")
    public ApiResponse<User> createUser(@RequestBody @Valid UserCreationRequest request) {
        log.info("API CALL: Yêu cầu đăng ký tài khoản mới với Username [{}]", request.getUserName());
        ApiResponse<User> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.createUser(request));
        return apiResponse;
    }

    // Read - Danh sách User
    @GetMapping("/admin")
    public ApiResponse<List<User>> getUsers() {
        log.debug("API CALL: Yêu cầu lấy danh sách toàn bộ User (Admin)");
        ApiResponse<List<User>> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.getUsers());
        return apiResponse;
    }

    // Read - Lấy 1 User
    @GetMapping("/admin/{userId}")
    public ApiResponse<User> getUser(@PathVariable("userId") String userId) {
        log.debug("API CALL: Yêu cầu lấy chi tiết User ID [{}] (Admin)", userId);
        ApiResponse<User> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.getUser(userId));
        return apiResponse;
    }

    // Update
    @PutMapping("/admin/{userId}")
    public ApiResponse<User> updateUser(@PathVariable("userId") String userId, @RequestBody @Valid UserUpdateRequest request) {
        log.info("API CALL: Yêu cầu cập nhật thông tin User ID [{}] (Admin)", userId);
        ApiResponse<User> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.updateUser(userId, request));
        return apiResponse;
    }

    // Cập nhật thông tin cá nhân (Dành cho User tự cập nhật)
    @PutMapping("/profile/{userName}")
    public ApiResponse<User> updateMyProfile(@PathVariable String userName, @RequestBody @Valid UserUpdateRequest request) {
        log.info("API CALL: User [{}] tự yêu cầu cập nhật thông tin cá nhân", userName);
        ApiResponse<User> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.updateMyProfile(userName, request));
        return apiResponse;
    }

    // Delete
    @DeleteMapping("/admin/{userName}")
    public ApiResponse<String> deleteUser(@PathVariable("userName") String userName) {
        log.warn("API CALL: CẢNH BÁO - Yêu cầu xóa User [{}] (Admin)", userName);
        User user = userService.getUserByUserName(userName);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.deleteUser(user.getId()));
        return apiResponse;
    }

    // Upgrade to Seller
    @PutMapping("/upgrade-to-seller/{userName}")
    public ApiResponse<String> upgradeToSeller(@PathVariable String userName) {
        log.info("API CALL: Yêu cầu nâng cấp User [{}] lên vai trò SELLER", userName);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.upgradeBidderToSeller(userName));
        return apiResponse;
    }

    // Đăng nhập
    @PostMapping("/login")
    public ApiResponse<User> login(@RequestBody LoginRequest request) {
        log.info("API CALL: Yêu cầu đăng nhập từ Username[{}]", request.getUserName());
        ApiResponse<User> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.login(request));
        return apiResponse;
    }

    // Quên mật khẩu - Xác thực
    @PostMapping("/verify-reset-info")
    public ApiResponse<String> verifyUserInfo(@RequestBody VerifyInfoRequest request) {
        log.info("API CALL: Yêu cầu xác thực thông tin Quên mật khẩu cho User [{}]", request.getUserName());
        ApiResponse<String> apiResponse = new ApiResponse<>();
        // API này sẽ trả về cái Mã bí mật
        apiResponse.setResult(userService.verifyUserInfo(request));
        return apiResponse;
    }

    // Quên mật khẩu - Đổi pass
    @PutMapping("/reset-password")
    public ApiResponse<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        log.info("API CALL: Yêu cầu cấp lại mật khẩu mới cho User [{}] bằng Token", request.getUserName());
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.resetPassword(request));
        return apiResponse;
    }

    // Lấy thông tin cá nhân bằng userName
    @GetMapping("/profile/{userName}")
    public ApiResponse<User> getMyProfile(@PathVariable String userName) {
        log.debug("API CALL: Yêu cầu lấy thông tin Profile của User [{}]", userName);
        ApiResponse<User> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.getUserByUserName(userName));
        return apiResponse;
    }

    // Nút Khóa / Mở khóa tài khoản
    @PutMapping("/admin/{userName}/ban")
    public ApiResponse<String> toggleBanUser(@PathVariable("userName") String userName) {
        log.warn("API CALL: Yêu cầu Khóa / Mở khóa tài khoản User [{}] (Admin)", userName);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.toggleBanUser(userName));
        return apiResponse;
    }

    // Upload avatar cho User
    @PostMapping("/{userName}/avatar")
    public ApiResponse<String> uploadAvatar(@PathVariable String userName, @RequestParam("file") MultipartFile file) throws IOException {
        log.info("API CALL: User [{}] yêu cầu tải lên ảnh Avatar mới", userName);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.uploadAvatar(userName, file));
        return apiResponse;
    }

    // Gỡ avatar ( đưa về avatar mặc định)
    @DeleteMapping("/{userName}/avatar")
    public ApiResponse<String> removeAvatar(@PathVariable String userName) {
        log.info("API CALL: User [{}] yêu cầu gỡ Avatar về mặc định", userName);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setCode(1000);
        apiResponse.setMessage("deleted avatar successfully");
        apiResponse.setResult(userService.removeAvatar(userName));
        return apiResponse;
    }

    // VIẾT ĐÁNH GIÁ
    @PostMapping("/reviews")
    public ApiResponse<String> addSellerReview(@RequestBody org.auctionfx.auctionbidsystemspringbootrework.dto.request.ReviewRequest request) {
        ApiResponse<String> response = new ApiResponse<>();
        response.setResult(userService.addSellerReview(request));
        return response;
    }

    // XEM ĐÁNH GIÁ (CÓ LOAD MORE)
    // Cách gọi: GET /users/LaziChicken7/reviews?page=0&size=5
    @GetMapping("/{sellerUsername}/reviews")
    public ApiResponse<List<org.auctionfx.auctionbidsystemspringbootrework.entity.user.SellerReview>> getSellerReviews(
            @PathVariable String sellerUsername,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        ApiResponse<List<org.auctionfx.auctionbidsystemspringbootrework.entity.user.SellerReview>> response = new ApiResponse<>();
        response.setResult(userService.getSellerReviews(sellerUsername, page, size));
        return response;
    }

    // API: Tìm kiếm người dùng
    @GetMapping("/search")
    public ApiResponse<List<User>> searchUsers(@RequestParam String keyword) {
        log.info("API CALL: Khách yêu cầu tìm kiếm người dùng bằng từ khóa [{}]", keyword);
        ApiResponse<List<User>> response = new ApiResponse<>();
        response.setResult(searchService.searchUsers(keyword));
        return response;
    }

    // API: Kiểm tra tình trạng online/offline của user
    @GetMapping("/{userName}/status")
    public ApiResponse<String> getUserStatus(@PathVariable String userName) {
        // Không dùng log.info ở đây để tránh làm trôi log console do Client gọi liên tục
        log.debug("API CALL: Kiểm tra trạng thái hoạt động của User [{}]", userName);
        ApiResponse<String> response = new ApiResponse<>();
        response.setResult(userService.getUserStatus(userName));
        return response;
    }
}
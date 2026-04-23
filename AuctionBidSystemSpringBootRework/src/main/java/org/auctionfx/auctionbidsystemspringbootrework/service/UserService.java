package org.auctionfx.auctionbidsystemspringbootrework.service;

import jakarta.transaction.Transactional;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.UserCreationRequest;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.UserUpdateRequest;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Admin;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Bidder;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.auctionfx.auctionbidsystemspringbootrework.exception.UserErrorCode;
import org.auctionfx.auctionbidsystemspringbootrework.exception.UserException;
import org.auctionfx.auctionbidsystemspringbootrework.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


import static org.auctionfx.auctionbidsystemspringbootrework.enums.Role.*;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    // CREATE
    // Request: Những thông tin cần thiết để tạo ra User
    // Để tạo ra một cái Request thì chúng ta cần package dto
    @Transactional
    public User createUser(UserCreationRequest request) {
        // 1. Kiểm tra trùng lặp
        if (userRepository.existsByUserName(request.getUserName())) {
            throw new UserException(UserErrorCode.USERNAME_EXISTED);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserException(UserErrorCode.EMAIL_EXISTED);
        }
        if (userRepository.existsByCitizenId(request.getCitizenId())) {
            throw new UserException(UserErrorCode.CITIZEN_ID_EXISTED);
        }
        if (userRepository.existsByNumberPhone(request.getNumberPhone())) {
            throw new UserException(UserErrorCode.PHONE_NUMBER_EXISTED);
        }

        // 2. Factory Pattern
        User newUser;
        switch (request.getRole()) {
            case ADMIN -> newUser = new Admin();
            case SELLER -> newUser = new User();
            case BIDDER -> newUser = new Bidder();
            default -> throw new IllegalStateException("Unexpected value: " + request.getRole());
        }

        // ==========================================
        // 3. LOGIC TẠO ID "USR1", "USR2" BẰNG TAY TẠI ĐÂY
        // ==========================================
        Integer maxId = userRepository.findMaxIdNumber();
        int nextIdNumber = (maxId == null ? 0 : maxId) + 1; // Nếu DB trống (null) thì bắt đầu từ 1
        newUser.setId("USR" + nextIdNumber);
        // ==========================================

        // 4. Set dữ liệu chung
        newUser.setUserName(request.getUserName());
        // Tạm thời mã hóa kiểu cũ của bạn (cộng 5 ASCII), sau này học Security sẽ đổi sang Bcrypt
        newUser.setPassword(encodePassword(request.getPassword()));
        newUser.setFullName(request.getFullName());
        newUser.setEmail(request.getEmail());
        newUser.setNumberPhone(request.getNumberPhone());
        newUser.setCitizenId(request.getCitizenId());
        newUser.setRole(request.getRole());

        // 5. Lưu xuống database
        return userRepository.save(newUser);
    }

    // READ
    // Lấy thông tin người dùng
    // Lấy tất cả thông tin
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    // Lấy thông tin của một người duy nhất
    public User getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }

    // UPDATE
    // Cập nhật thông tin người dùng
    public User updateUser(String userId, UserUpdateRequest request) {
        User user = getUser(userId);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserException(UserErrorCode.EMAIL_EXISTED);
        }
        if (userRepository.existsByNumberPhone(request.getNumberPhone())) {
            throw new UserException(UserErrorCode.PHONE_NUMBER_EXISTED);
        }

        user.setFullName(request.getFullName());
        user.setPassword(encodePassword(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNumberPhone(request.getNumberPhone());

        return userRepository.save(user);
    }


    // DELETE
    // Xóa người dùng
    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }

    // MÃ HÓA VÀ GIẢI MÃ PASSWORD

    private static String encodePassword(String password) {
        StringBuilder encoded = new StringBuilder();
        for (char c : password.toCharArray()) {
            encoded.append((char) (c + 5));
        }
        return encoded.toString();
    }

    public static String decode(String encodedPassword) {
        StringBuilder decoded = new StringBuilder();
        for (char c : encodedPassword.toCharArray()) {
            decoded.append((char) (c - 5));
        }
        return decoded.toString();
    }

}
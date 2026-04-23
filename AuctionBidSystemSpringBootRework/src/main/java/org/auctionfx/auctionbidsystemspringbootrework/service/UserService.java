package org.auctionfx.auctionbidsystemspringbootrework.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.UserCreationRequest;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.UserUpdateRequest;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Admin;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Bidder;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.auctionfx.auctionbidsystemspringbootrework.enums.Role;
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

    @Autowired
    private EntityManager entityManager; // Công cụ quản lý Cache của Spring Boot

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
        String prefixCode = "";
        switch (request.getRole()) {
            case ADMIN -> {
                newUser = new Admin();
                prefixCode = "ADM";
            }
            case SELLER -> {
                newUser = new User();
                prefixCode = "USR";
            }
            case BIDDER -> {
                newUser = new Bidder();
                prefixCode = "BID";
            }
            default -> throw new IllegalStateException("Unexpected value: " + request.getRole());
        }

        // 3. LOGIC TẠO MÃ HIỂN THỊ (USER CODE)
        Integer maxId = userRepository.findMaxUserCodeNumber(prefixCode);
        int nextIdNumber = (maxId == null ? 0 : maxId) + 1;
        newUser.setUserCode(prefixCode + nextIdNumber); // Kết quả sẽ là BID1, SLR1...
        // ----------------------------------------

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

    @Transactional
    public String upgradeBidderToSeller(String userName) {
        // 1. Lấy user từ DB lên
        User user = userRepository.findByUserName(userName);

        // 2. Kiểm tra điều kiện
        if (user == null) {
            throw new UserException(UserErrorCode.USER_NOT_FOUND);
        }

        switch (user.getRole()) {
            case SELLER -> throw new UserException(UserErrorCode.USER_ALREADY_SELLER);
            case ADMIN -> throw new UserException(UserErrorCode.USER_CONFLICT_UPGRADE);
        }

        // 3. LOGIC ĐỔI MÃ TỪ BID SANG SLR ---
        Integer maxId = userRepository.findMaxUserCodeNumber("SLR");
        int nextIdNumber = (maxId == null ? 0 : maxId) + 1;
        String newSellerCode = "SLR" + nextIdNumber;

        // 4. Chạy lệnh cập nhật xuống DB
        userRepository.upgradeToSellerAndUpdateCode(user.getId(), newSellerCode);
        userRepository.insertIntoSellersTable(user.getId());

        // 5. Xóa cache của JPA để lần tới gọi findByUserName, nó sẽ load lại dữ liệu thành đối tượng class Seller.
        entityManager.clear();

        return "Upgrade successfully! Your new code is: " + newSellerCode;
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
    public String deleteUser(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserException(UserErrorCode.USER_NOT_FOUND);
        }
        userRepository.deleteById(userId);
        return "User deleted successfully!";
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

/*
Lưu ý: Trong Spring Boot, nó có một bộ nhớ đệm (Cache) gọi là EntityManager.
Nếu bạn đổi database bằng SQL thuần, bộ nhớ đệm sẽ không biết, dẫn đến lúc lấy User ra nó vẫn nghĩ đó là Bidder.
Ta phải dùng entityManager.clear() để "tẩy não" nó.
 */
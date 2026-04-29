package org.auctionfx.auctionbidsystemspringbootrework.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.ResetPasswordRequest;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.UserCreationRequest;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.UserUpdateRequest;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.VerifyInfoRequest;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Admin;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Bidder;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Seller;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.auctionfx.auctionbidsystemspringbootrework.exception.ErrorCode;
import org.auctionfx.auctionbidsystemspringbootrework.exception.UserException;
import org.auctionfx.auctionbidsystemspringbootrework.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager; // Công cụ quản lý Cache của Spring Boot

    // Khay chứa Mã bí mật (Key là Username, Value là Mã Token)
    private final Map<String, String> resetTokens = new ConcurrentHashMap<>();

    // CREATE
    // Request: Những thông tin cần thiết để tạo ra User
    // Để tạo ra một cái Request thì chúng ta cần package dto
    @Transactional
    public User createUser(UserCreationRequest request) {
        // 1. Kiểm tra trùng lặp
        if (userRepository.existsByUserName(request.getUserName())) {
            throw new UserException(ErrorCode.USERNAME_EXISTED);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserException(ErrorCode.EMAIL_EXISTED);
        }
        if (userRepository.existsByCitizenId(request.getCitizenId())) {
            throw new UserException(ErrorCode.CITIZEN_ID_EXISTED);
        }
        if (userRepository.existsByNumberPhone(request.getNumberPhone())) {
            throw new UserException(ErrorCode.PHONE_NUMBER_EXISTED);
        }

        // 2. Factory Pattern
        User newUser;
        String prefixCode = "";
        switch (request.getRole()) {
            case ADMIN -> {
                newUser = new Admin();
                prefixCode = "ADM";
            }
            case BIDDER -> {
                newUser = new Bidder();
                prefixCode = "BID";
            }
            case SELLER -> {
                newUser = new Seller();
                prefixCode = "SLR";
            }
            default -> throw new IllegalStateException("Unexpected value: " + request.getRole());
        }

        // 3. LOGIC TẠO MÃ HIỂN THỊ (USER CODE CHUNG TOÀN HỆ THỐNG)
        // Không truyền prefixCode vào hàm tìm max nữa
        Integer maxId = userRepository.findMaxUserCodeNumber();
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
        // 1. Lấy user từ DB lên (Lúc này Java vẫn coi đây là Bidder)
        User user = userRepository.findByUserName(userName);

        if (user == null) {
            throw new UserException(ErrorCode.USER_NOT_FOUND);
        }

        switch (user.getRole()) {
            case SELLER -> throw new UserException(ErrorCode.USER_ALREADY_SELLER);
            case ADMIN -> throw new UserException(ErrorCode.USER_CONFLICT_UPGRADE);
            default -> { break; }
        }

        // 2. Sinh mã mới
        String newSellerCode = "SLR" + user.getUserCode().substring(3);

        // 3. Chạy lệnh SQL can thiệp trực tiếp vào MySQL
        userRepository.upgradeToSellerAndUpdateCode(user.getId(), newSellerCode);
        userRepository.insertIntoSellersTable(user.getId());

        // =========================================================
        // 4. BƯỚC QUYẾT ĐỊNH: ĐỒNG BỘ LẠI ĐỐI TƯỢNG TRONG JAVA
        // =========================================================

        // Bắt buộc đẩy ngay các lệnh SQL ở trên xuống DB ngay tắp lự
        entityManager.flush();

        // Xóa sạch bộ nhớ đệm cũ (Xóa cái xác Bidder cũ đi)
        entityManager.clear();

        // GỌI LẠI đối tượng từ DB lên.
        // Lần này Spring Boot sẽ thấy dòng dữ liệu trong bảng 'sellers' và TỰ ĐỘNG khởi tạo nó là class SELLER.
        User upgradedUser = userRepository.findById(user.getId()).orElse(null);

        // Code test thử để in ra màn hình Console xem nó đã thực sự là Seller chưa:
        if (upgradedUser instanceof Seller) {
            System.out.println("Successfully: Java has been recognized that user is Bidder");
            System.out.println("Seller rating now: " + ((Seller) upgradedUser).getRating());
        }

        return "Upgrade successfully! Your new code is: " + newSellerCode;
    }

    // QUÊN MẬT KHẨU
    // 1. Xác thực thông tin (trả về token)
    public String verifyUserInfo(VerifyInfoRequest request) {
        User user = userRepository.findByUserName(request.getUserName());

        // Kiểm tra xem User có tồn tại không, Email và CCCD có khớp 100% không?
        if (user == null ||
                !user.getEmail().equals(request.getEmail()) ||
                !user.getCitizenId().equals(request.getCitizenId())) {
            throw new UserException(ErrorCode.USER_INFO_NOT_MATCH);
        }

        // Nếu khớp -> Tạo ra một UUID
        String token = UUID.randomUUID().toString();

        // Cất mã đó vào trong Map
        resetTokens.put(user.getUserName(), token);

        return token; // Trả mã này về cho Giao diện JavaFX/Postman
    }

    // 2. Tiến hành đổi mật khẩu
    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        // Lấy cái mã bí mật trong Map ra kiểm tra
        String savedToken = resetTokens.get(request.getUserName());

        // Nếu mã không khớp hoặc mã không tồn tại -> Hacker đang cố vượt rào
        if (savedToken == null || !savedToken.equals(request.getResetToken())) {
            throw new UserException(ErrorCode.INVALID_RESET_TOKEN);
        }

        // Nếu đúng mã -> Đổi mật khẩu
        User user = userRepository.findByUserName(request.getUserName());
        user.setPassword(encodePassword(request.getNewPassword()));
        userRepository.save(user);

        // Đổi xong thì XÓA mã bí mật đó đi (Mỗi mã chỉ được dùng 1 lần)
        resetTokens.remove(request.getUserName());

        return "Password change successfully!";
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
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
    }

    // UPDATE
    // Cập nhật thông tin người dùng
    public User updateUser(String userId, UserUpdateRequest request) {
        User user = getUser(userId);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserException(ErrorCode.EMAIL_EXISTED);
        }
        if (userRepository.existsByNumberPhone(request.getNumberPhone())) {
            throw new UserException(ErrorCode.PHONE_NUMBER_EXISTED);
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
            throw new UserException(ErrorCode.USER_NOT_FOUND);
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
package org.auctionfx.auctionbidsystemspringbootrework.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.*;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Admin;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Bidder;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Seller;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.auctionfx.auctionbidsystemspringbootrework.enums.Role;
import org.auctionfx.auctionbidsystemspringbootrework.exception.ErrorCode;
import org.auctionfx.auctionbidsystemspringbootrework.exception.UserException;
import org.auctionfx.auctionbidsystemspringbootrework.repository.BidderRepository;
import org.auctionfx.auctionbidsystemspringbootrework.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j // Kích hoạt bộ ghi log của Lombok
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager; // Công cụ quản lý Cache của Spring Boot

    @Autowired
    private BidderRepository bidderRepository;

    // Khay chứa Mã bí mật (Key là Username, Value là Mã Token)
    private final Map<String, String> resetTokens = new ConcurrentHashMap<>();

    // CREATE
    // Request: Những thông tin cần thiết để tạo ra User
    // Để tạo ra một cái Request thì chúng ta cần package dto
    @Transactional
    public User createUser(UserCreationRequest request) {
        log.info("SERVICE: Bắt đầu tiến trình tạo mới User [{}] với vai trò [{}]", request.getUserName(), request.getRole());

        // 1. Kiểm tra trùng lặp
        if (userRepository.existsByUserName(request.getUserName())) {
            log.error("Lỗi đăng ký: Username [{}] đã tồn tại", request.getUserName());
            throw new UserException(ErrorCode.USERNAME_EXISTED);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.error("Lỗi đăng ký: Email [{}] đã tồn tại", request.getEmail());
            throw new UserException(ErrorCode.EMAIL_EXISTED);
        }
        if (userRepository.existsByCitizenId(request.getCitizenId())) {
            log.error("Lỗi đăng ký: CCCD [{}] đã tồn tại", request.getCitizenId());
            throw new UserException(ErrorCode.CITIZEN_ID_EXISTED);
        }
        if (userRepository.existsByNumberPhone(request.getNumberPhone())) {
            log.error("Lỗi đăng ký: Số điện thoại [{}] đã tồn tại", request.getNumberPhone());
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
                Bidder bidder = new Bidder();
                // Sinh số tài khoản rồi gán vào
                bidder.setBankAccountNumber(generateUniqueBankAccountNumber());
                newUser = bidder;
                prefixCode = "BID";
            }
            case SELLER -> {
                Seller seller = new Seller();
                // Sinh số tài khoản rồi gán vào
                seller.setBankAccountNumber(generateUniqueBankAccountNumber());
                newUser = seller;
                prefixCode = "SLR";
            }
            default -> {
                log.error("Vai trò truyền vào không hợp lệ: {}", request.getRole());
                throw new IllegalStateException("Unexpected value: " + request.getRole());
            }
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

        // 5.Set avatar mặc định cho tất cả user mới
        newUser.setAvatarUrl("/uploads/images/avatar/avatarmacdinh.png"); // Đã sửa lại đường dẫn cho chuẩn

        // 6. Lưu xuống database
        User savedUser = userRepository.save(newUser);
        log.info("Tạo User thành công! UserCode được cấp phát: {}", savedUser.getUserCode());
        return savedUser;
    }

    // Đăng nhập
    @Transactional
    public User login(LoginRequest request) {
        log.info("SERVICE: Đang xác thực đăng nhập cho Username [{}]", request.getUserName());
        User user = userRepository.findByUserName(request.getUserName());
        if (user == null) {
            log.error("Lỗi đăng nhập: Không tìm thấy Username [{}]", request.getUserName());
            throw new UserException(ErrorCode.USERNAME_NOT_FOUND);
        }

        // Kiểm tra xem tài khoản có bị ban không
        if (user.isBanned()) {
            log.warn("Từ chối đăng nhập: Tài khoản [{}] đang bị khóa", request.getUserName());
            throw new UserException(ErrorCode.USER_BANNED);
        }

        // Kiểm tra mật khẩu (Sử dụng hàm mã hóa đang có sẵn trong file của bạn)
        if (!user.getPassword().equals(encodePassword(request.getPassword()))) {
            log.error("Lỗi đăng nhập: Sai mật khẩu cho Username [{}]", request.getUserName());
            throw new UserException(ErrorCode.PASSWORD_NOT_MATCH); // Hoặc tạo mã lỗi WRONG_PASSWORD
        }
        log.info("Đăng nhập thành công! Chào mừng [{}]", request.getUserName());
        return user;
    }

    @Transactional
    public String upgradeBidderToSeller(String userName) {
        log.info("SERVICE: Tiến hành nâng cấp User [{}] lên Seller", userName);
        // 1. Lấy user từ DB lên (Lúc này Java vẫn coi đây là Bidder)
        User user = userRepository.findByUserName(userName);

        if (user == null) {
            log.error("Lỗi nâng cấp: Không tìm thấy User [{}]", userName);
            throw new UserException(ErrorCode.USER_NOT_FOUND);
        }

        switch (user.getRole()) {
            case SELLER -> {
                log.warn("Hủy nâng cấp: User [{}] đã là SELLER từ trước", userName);
                throw new UserException(ErrorCode.USER_ALREADY_SELLER);
            }
            case ADMIN -> {
                log.error("Lỗi nâng cấp: Không thể nâng cấp ADMIN [{}] thành SELLER", userName);
                throw new UserException(ErrorCode.USER_CONFLICT_UPGRADE);
            }
            default -> { break; }
        }

        // 2. Sinh mã mới
        String newSellerCode = "SLR" + user.getUserCode().substring(3);

        // 3. Chạy lệnh SQL can thiệp trực tiếp vào MySQL
        userRepository.upgradeToSellerAndUpdateCode(user.getId(), newSellerCode);
        userRepository.insertIntoSellersTable(user.getId());
        log.info("Đã chạy lệnh SQL ghi đè xuống Database cho User [{}]", userName);

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
            log.info("Successfully: Java has been recognized that user is Seller");
            log.info("Seller rating now: {}", ((Seller) upgradedUser).getRating());
        }

        return "Upgrade successfully! Your new code is: " + newSellerCode;
    }

    // QUÊN MẬT KHẨU
    // 1. Xác thực thông tin (trả về token)
    @Transactional
    public String verifyUserInfo(VerifyInfoRequest request) {
        log.info("SERVICE: Bắt đầu xác thực thông tin cấp lại mật khẩu cho User[{}]", request.getUserName());
        User user = userRepository.findByUserName(request.getUserName());

        // Kiểm tra xem User có tồn tại không, Email và CCCD có khớp 100% không?
        if (user == null ||
                !user.getEmail().equals(request.getEmail()) ||
                !user.getCitizenId().equals(request.getCitizenId())) {
            log.error("Xác thực thất bại: Thông tin cung cấp không khớp với dữ liệu User [{}]", request.getUserName());
            throw new UserException(ErrorCode.USER_INFO_NOT_MATCH);
        }

        // Nếu khớp -> Tạo ra một UUID
        String token = UUID.randomUUID().toString();

        // Cất mã đó vào trong Map
        resetTokens.put(user.getUserName(), token);
        log.info("Xác thực thành công. Đã sinh Token cấp lại mật khẩu cho User [{}]", request.getUserName());

        return token; // Trả mã này về cho Giao diện JavaFX/Postman
    }

    // 2. Tiến hành đổi mật khẩu
    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        log.info("SERVICE: Tiến hành đổi mật khẩu cho User [{}] bằng Token", request.getUserName());
        // Lấy cái mã bí mật trong Map ra kiểm tra
        String savedToken = resetTokens.get(request.getUserName());

        // Nếu mã không khớp hoặc mã không tồn tại -> Hacker đang cố vượt rào
        if (savedToken == null || !savedToken.equals(request.getResetToken())) {
            log.warn("CẢNH BÁO BẢO MẬT: Phát hiện mã Token không hợp lệ hoặc đã hết hạn từ request của User [{}]", request.getUserName());
            throw new UserException(ErrorCode.INVALID_RESET_TOKEN);
        }

        // Nếu đúng mã -> Đổi mật khẩu
        User user = userRepository.findByUserName(request.getUserName());
        user.setPassword(encodePassword(request.getNewPassword()));
        userRepository.save(user);

        // Đổi xong thì XÓA mã bí mật đó đi (Mỗi mã chỉ được dùng 1 lần)
        resetTokens.remove(request.getUserName());
        log.info("Đổi mật khẩu thành công cho User [{}]. Đã thu hồi Token.", request.getUserName());

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
                .orElseThrow(() -> {
                    log.error("Không tìm thấy User với ID [{}]", userId);
                    return new UserException(ErrorCode.USER_NOT_FOUND);
                });
    }

    // Save user (dùng cho update avatar cho nhanh, không cần dài như update)
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    // UPDATE
    // Cập nhật thông tin người dùng
    public User updateUser(String userId, UserUpdateRequest request) {
        log.info("SERVICE: Admin đang cập nhật thông tin cho User ID [{}]", userId);
        User user = getUser(userId);

        if (userRepository.existsByEmail(request.getEmail()) && !user.getEmail().equals(request.getEmail())) {
            log.error("Cập nhật thất bại: Email [{}] đã được người khác sử dụng", request.getEmail());
            throw new UserException(ErrorCode.EMAIL_EXISTED);
        }
        if (userRepository.existsByNumberPhone(request.getNumberPhone()) && !user.getNumberPhone().equals(request.getNumberPhone())) {
            log.error("Cập nhật thất bại: Số điện thoại [{}] đã được người khác sử dụng", request.getNumberPhone());
            throw new UserException(ErrorCode.PHONE_NUMBER_EXISTED);
        }

        user.setFullName(request.getFullName());
        user.setPassword(encodePassword(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNumberPhone(request.getNumberPhone());
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        log.info("Cập nhật thông tin thành công cho User ID [{}]", userId);
        return userRepository.save(user);
    }

    @Transactional
    public User updateMyProfile(String userName, UserUpdateRequest request) {
        log.info("SERVICE: Xử lý cập nhật thông tin cá nhân cho User [{}]", userName);
        User user = userRepository.findByUserName(userName);
        if (user == null) {
            log.error("Lỗi: Không tìm thấy User [{}]", userName);
            throw new UserException(ErrorCode.USER_NOT_FOUND);
        }

        // Kiểm tra xem Email có bị trùng với người khác không
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            log.error("Cập nhật thất bại: Email [{}] đã bị trùng", request.getEmail());
            throw new UserException(ErrorCode.EMAIL_EXISTED);
        }

        // Cập nhật thông tin cơ bản
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        // Nếu người dùng có nhập mật khẩu mới thì mới tiến hành đổi
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            log.info("User [{}] đã tiến hành thay đổi cả mật khẩu", userName);
            user.setPassword(encodePassword(request.getPassword()));
        }

        log.info("Cập nhật thông tin cá nhân thành công cho User [{}]", userName);
        return userRepository.save(user);
    }

    // DELETE
    // Xóa người dùng
    public String deleteUser(String userId) {
        log.warn("SERVICE: Tiến hành XÓA User ID [{}] khỏi CSDL", userId);
        if (!userRepository.existsById(userId)) {
            log.error("Lỗi xóa: Không tìm thấy User ID [{}]", userId);
            throw new UserException(ErrorCode.USER_NOT_FOUND);
        }
        userRepository.deleteById(userId);
        log.info("Xóa thành công User ID [{}]", userId);
        return "User deleted successfully!";
    }

    // PROFILE
    // Lấy thông tin Profile của người dùng
    public User getUserByUserName(String userName) {
        User user = userRepository.findByUserName(userName);
        if (user == null) {
            // Ném ra Exception chuẩn của hệ thống mà bạn đã cấu hình từ trước
            log.error("Lỗi lấy Profile: Không tìm thấy Username [{}]", userName);
            throw new UserException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
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

    // Hàm sinh số tài khoản ngẫu nhiên 10 chữ số (Ví dụ: 1045628193)
    private String generateUniqueBankAccountNumber() {
        String accNo;
        do {
            long randomNum = 1000000000L + (long)(Math.random() * 8999999999L);
            accNo = String.valueOf(randomNum);
        } while (bidderRepository.existsByBankAccountNumber(accNo)); // Vòng lặp chạy lại nếu bị trùng
        return accNo;
    }

    @Transactional
    public String toggleBanUser(String userName) {
        log.info("SERVICE: Xử lý thay đổi trạng thái BAN cho User [{}]", userName);
        // TÌM THEO userName THAY VÌ id
        User user = userRepository.findByUserName(userName);

        if (user == null) {
            log.error("Lỗi đổi trạng thái Ban: Không tìm thấy User [{}]", userName);
            throw new UserException(ErrorCode.USER_NOT_FOUND);
        }

        if (user.getRole() == Role.ADMIN) {
            log.error("Lỗi đổi trạng thái Ban: KHÔNG ĐƯỢC PHÉP Ban tài khoản Admin [{}]", userName);
            throw new UserException(ErrorCode.BAN_USER_INVALID);
        }

        user.setBanned(!user.isBanned()); // Đảo ngược trạng thái
        userRepository.save(user);

        String result = user.isBanned() ? "User has been banned!" : "User has been unbanned!";
        log.info("Hoàn tất: {}", result);
        return result;
    }

    // Upload avatar cho User
    @Transactional
    public String uploadAvatar(String userName, MultipartFile file) throws IOException {
        log.info("SERVICE: Bắt đầu xử lý lưu file Avatar cho User [{}]", userName);
        // Kiểm tra file
        if (file == null || file.isEmpty()) {
            log.error("Lỗi upload: File trống hoặc bị Null");
            throw new IOException("No file uploaded or file is empty");
        }

        // Kiểm tra user có tồn tại trong database không
        User user = getUserByUserName(userName);

        // 1. ĐỔI ĐƯỜNG DẪN LƯU THÀNH THƯ MỤC AVATAR
        String uploadDir = "uploads/images/avatar/";
        File dir = new File(uploadDir);
        if (!dir.exists() && !dir.mkdirs()) {
            log.error("LỖI HỆ THỐNG: Không thể tạo thư mục lưu trữ Avatar tại [{}]", uploadDir);
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
        userRepository.save(user);

        log.info("Lưu file Avatar thành công cho User [{}]. Đường dẫn: {}", userName, fileUrl);
        // Trả về message thành công
        return "Avatar uploaded successfully: " + fileUrl;
    }

    // Gỡ avatar ( đưa về avatar mặc định)
    @Transactional
    public String removeAvatar(String userName) {
        log.info("SERVICE: Xử lý gỡ bỏ Avatar, đưa về mặc định cho User [{}]", userName);
        // lấy user từ database
        User user = getUserByUserName(userName);

        // LƯU Ý: Phải có dấu "/" ở đầu chuỗi để Frontend ghép link không bị lỗi
        user.setAvatarUrl("/uploads/images/avatar/avatarmacdinh.png");

        // Dùng hàm lưu nhanh user
        userRepository.save(user);

        log.info("Đã gỡ Avatar thành công cho User [{}]", userName);
        return user.getAvatarUrl();
    }

}

/*
Lưu ý: Trong Spring Boot, nó có một bộ nhớ đệm (Cache) gọi là EntityManager.
Nếu bạn đổi database bằng SQL thuần, bộ nhớ đệm sẽ không biết, dẫn đến lúc lấy User ra nó vẫn nghĩ đó là Bidder.
Ta phải dùng entityManager.clear() để "tẩy não" nó.
 */
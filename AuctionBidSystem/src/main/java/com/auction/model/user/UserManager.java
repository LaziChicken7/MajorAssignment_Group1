package com.auction.model.user;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.auction.model.enums.Role;
import com.auction.model.item.Item;

class RegisteredFailException extends Exception {
    public RegisteredFailException(String msg) { super(msg); }
}

class ForgotPasswordException extends Exception {
    public ForgotPasswordException(String msg) { super(msg); }
}

public class UserManager {

    // Chuyển sang dùng ConcurrentHashMap (Key là userName, Value là đối tượng User)
    // Map an toàn đa luồng, không cần lock khi tìm kiếm!
    private static Map<String, User> users = new ConcurrentHashMap<>();

    // Dùng nội bộ để tìm user
    private static User findUser(String userName) {
        return users.get(userName);
    }

    private static Bidder findBidder(String userName) {
        User u = findUser(userName);
        if (u instanceof Bidder) return (Bidder) u;
        return null;
    }

    // LOGIC CHÍNH

    // Khai báo Lock bảo vệ việc Đăng ký
    private static final ReentrantLock registerLock = new ReentrantLock();
    public boolean Register(String userName, String password, String fullName, String email, String numberPhone, String citizenId) throws RegisteredFailException {
        registerLock.lock();
        try {
            if (users.containsKey(userName)) { throw new RegisteredFailException("Tài khoản đã tồn tại"); }
            // Lặp để check Email, CCCD, SĐT
            for (User user : users.values()) {
                if (user.getEmail().equals(email)) { throw new RegisteredFailException("Email đã tồn tại"); }
                if (user.getCitizenId().equals(citizenId)) { throw new RegisteredFailException("CCCD đã tồn tại"); }
                if (user.getNumberPhone().equals(numberPhone)) { throw new RegisteredFailException("Số điện thoại đã tồn tại"); }
            }

            if (isWeakPassword(password)) {
                throw new RegisteredFailException("Mật khẩu yếu (Cần ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và số)");
            }

            User newUser = UserFactory.createUser(Role.BIDDER, userName, password, fullName, email, numberPhone, citizenId);
            users.put(userName, newUser);
            return true;
        } finally {
            registerLock.unlock();
        }
    }

    public boolean Login(String userName, String password) {
        User user = findUser(userName);
        if (user != null && user.getPassword().equals(User.encode(password))) {
            System.err.println("Đăng nhập thành công");
            return true;
        }
        System.out.println("Sai tài khoản hoặc mật khẩu");
        return false;
    }

    public boolean ForgotPassword(String userName, String email, String newPassword) {
        User user = findUser(userName);
        if (user != null && user.getEmail().equals(email)) {
            if (isWeakPassword(newPassword)) {
                System.err.println("Mật khẩu mới quá yếu");
                return false;
            }
            user.setPassword(newPassword);
            System.err.println("Đổi mật khẩu thành công");
            return true;
        }
        System.err.println("Không tìm thấy tài khoản");
        return false;
    }

    private boolean isWeakPassword(String password) {
        return password.length() < 8 || !password.matches(".*[A-Z].*") ||
                !password.matches(".*[a-z].*") || !password.matches(".*\\d.*");
    }

    // GETTER & SETTER

    public User getUser(String userName) { return findUser(userName); }

    public static void updateFullName(String userName, String fullName) {
        User u = findUser(userName);
        if (u != null) u.setFullName(fullName);
    }

    public static void updateEmail(String userName, String email) {
        User u = findUser(userName);
        if (u != null) u.setEmail(email);
    }

    public static void updateNumberPhone(String userName, String numberPhone) {
        User u = findUser(userName);
        if (u != null) u.setNumberPhone(numberPhone);
    }

    // --- QUẢN LÝ TIỀN NONG & ĐẤU GIÁ (Dành riêng cho Bidder/Seller) ---

    public static void setMoneyinFrozen(String userName, BigDecimal money) {
        Bidder b = findBidder(userName);
        if (b != null) b.setMoneyinFrozen(money);
    }

    public static BigDecimal getMoneyinFrozen(String userName) {
        Bidder b = findBidder(userName);
        return (b != null) ? b.getMoneyinFrozen() : BigDecimal.ZERO;
    }

    public static void setMoneyOnWallet(String userName, BigDecimal money) {
        Bidder b = findBidder(userName);
        if (b != null) b.setMoneyOnWallet(money);
    }

    public static BigDecimal getMoneyOnWallet(String userName) {
        Bidder b = findBidder(userName);
        return (b != null) ? b.getMoneyOnWallet() : BigDecimal.ZERO;
    }

    public static void setSuccessBidItem(String userName, Item item) {
        Bidder b = findBidder(userName);
        if (b != null) b.addSuccessBidItem(item);
    }

    public static void setFailedBidItem(String userName, Item item) {
        Bidder b = findBidder(userName);
        if (b != null) b.addFailedBidItem(item);
    }
}
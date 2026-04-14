package com.auction.model.user;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import com.auction.model.enums.Role;
import com.auction.model.item.Item;

public class UserManager {

    private static List<User> users = new ArrayList<>();

    // Dùng nội bộ để tìm user
    private static User findUser(String userName) {
        for (User user : users) {
            if (user.getUserName().equals(userName)) return user;
        }
        return null;
    }

    private static Bidder findBidder(String userName) {
        User u = findUser(userName);
        if (u instanceof Bidder) return (Bidder) u;
        return null;
    }

    // LOGIC CHÍNH

    public boolean Register(String userName, String password, String fullName, String email, String numberPhone, String citizenId) {
        for (User user : users) {
            if (user.getUserName().equals(userName)) { System.err.println("Tài khoản đã tồn tại"); return false; }
            if (user.getEmail().equals(email)) { System.err.println("Email đã tồn tại"); return false; }
            if (user.getNumberPhone().equals(numberPhone)) { System.err.println("Số điện thoại đã tồn tại"); return false; }
            if (user.getCitizenId().equals(citizenId)) { System.err.println("CCCD đã tồn tại"); return false; }
        }

        if (!checkWeakPassword(password)) {
            System.err.println("Mật khẩu yếu");
            return false;
        }

        // Dùng Factory Method
        User newUser = UserFactory.createUser(Role.BIDDER, userName, password, fullName, email, numberPhone, citizenId);
        users.add(newUser);

        System.err.println("Đăng ký thành công");
        return true;
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
            user.setPassword(newPassword);
            System.err.println("Đổi mật khẩu thành công");
            return true;
        }
        System.err.println("Không tìm thấy tài khoản");
        return false;
    }

    private boolean checkWeakPassword(String password) {
        return password.length() >= 8 && password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") && password.matches(".*\\d.*");
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
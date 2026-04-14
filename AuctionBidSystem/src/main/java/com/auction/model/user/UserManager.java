package com.auction.model.user;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import com.auction.model.enums.Role;
import com.auction.model.item.Item;

class RegisteredFailException extends Exception {
    public RegisteredFailException(String msg) { super(msg); }
}

class ForgotPasswordException extends Exception {
    public ForgotPasswordException(String msg) { super(msg); }
}

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

    public boolean Register(String userName, String password, String fullName, String email, String numberPhone, String citizenId) throws RegisteredFailException {
        for (User user : users) {
            if (user.getUserName().equals(userName)) { throw new RegisteredFailException("Tài khoản đã tồn tại"); }
            if (user.getEmail().equals(email)) { throw new RegisteredFailException("Email đã tồn tại"); }
            if (user.getNumberPhone().equals(numberPhone)) { throw new RegisteredFailException("Số điện thoại đã tồn tại"); }
            if (user.getCitizenId().equals(citizenId)) { throw new RegisteredFailException("CCCD đã tồn tại"); }
        }

        if (!checkWeakPassword(password)) { throw new RegisteredFailException("Mật khẩu yếu"); }

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

    public boolean ForgotPassword(String userName, String email, String newPassword) throws ForgotPasswordException {
        User user = findUser(userName);

        if (user == null) {
            throw new ForgotPasswordException("Tài khoản không tồn tại trong hệ thống.");
        }

        if (!user.getEmail().equals(email)) {
            throw new ForgotPasswordException("Email xác thực không khớp với tài khoản.");
        }

        // Tái sử dụng hàm checkWeakPassword cho mật khẩu mới
        if (!checkWeakPassword(newPassword)) {
            throw new ForgotPasswordException("Mật khẩu mới quá yếu. Cần ít nhất 8 ký tự, có chữ hoa, chữ thường và số.");
        }

        // Nếu qua hết các vòng gửi xe trên
        user.setPassword(User.encode(newPassword)); // Nhớ encode cả mật khẩu mới nhé (nếu hàm setPassword chưa tự encode)
        System.out.println("Đổi mật khẩu thành công");
        return true;
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
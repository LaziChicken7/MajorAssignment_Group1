package com.auction.model.auction;

import com.auction.model.user.UserManager;

import java.math.BigDecimal;

// Tạo một class NotEnoughMoneyException để ném ra ngoại lệ khi số tiền không đủ
class NotEnoughMoneyException extends Exception {
    public NotEnoughMoneyException(String msg) {
        super(msg);
    }
}

// Chịu trách nhiệm về việc payment (thanh toán)
public abstract class PaymentService {
    // Các chức năng nạp rút tiền cơ bản trong thanh toán
    public static void addFreezeMoney(String userId, BigDecimal amount) {
        BigDecimal newValue = UserManager.getMoneyinFrozen(userId).add(amount);
        UserManager.setMoneyinFrozen(userId, newValue);
    }

    public static void subFreezeMoney(String userId, BigDecimal amount) throws NotEnoughMoneyException {
        BigDecimal userFreezeMoney = UserManager.getMoneyinFrozen(userId);
        if (userFreezeMoney.compareTo(amount) >= 0) {
            BigDecimal newValue = UserManager.getMoneyinFrozen(userId).subtract(amount);
            UserManager.setMoneyinFrozen(userId, newValue);
        } else {
            throw new NotEnoughMoneyException("Số tiền trong ví đóng băng không đủ");
        }
    }

    public static void addWalletMoney(String userId, BigDecimal amount) {
        BigDecimal newValue = UserManager.getMoneyOnWallet(userId).add(amount);
        UserManager.setMoneyOnWallet(userId, newValue);
    }

    public static void subWalletMoney(String userId, BigDecimal amount) throws NotEnoughMoneyException {
        BigDecimal userWalletMoney = UserManager.getMoneyOnWallet(userId);
        if (userWalletMoney.compareTo(amount) >= 0) {
            BigDecimal newValue = UserManager.getMoneyOnWallet(userId).subtract(amount);
            UserManager.setMoneyOnWallet(userId, newValue);
        } else {
            throw new NotEnoughMoneyException("Số tiền trong ví không đủ");
        }
    }

    // ------------------------------------------------------------------------------------
    // Chức năng đóng băng tiền

    // Đóng băng số tiền
    public static void FreezeMoney(String userId, BigDecimal amount) throws NotEnoughMoneyException {
        try {
            subWalletMoney(userId, amount);
            addFreezeMoney(userId, amount);
        } catch (NotEnoughMoneyException e) {
            // Sửa trong controller sau
            System.err.println(e.getMessage());
        }
    }

    // Dừng đóng băng tiền
    public static void unFreezeMoney(String userId, BigDecimal amount) throws NotEnoughMoneyException {
        try {
            subFreezeMoney(userId, amount);
            addWalletMoney(userId, amount);
        } catch (NotEnoughMoneyException e) {
            // Sửa trong controller sau
            System.err.println(e.getMessage());
        }
    }

    // Chuyển tiền đóng băng (đấu giá thành công)
    public static void transferMoney(String fromUserId, String toUserId, BigDecimal amount) throws NotEnoughMoneyException {
        try {
            subFreezeMoney(fromUserId, amount);
            addWalletMoney(toUserId, amount);
        } catch (NotEnoughMoneyException e) {
            // Sửa trong controller sau
            System.err.println(e.getMessage());
        }
    }
}
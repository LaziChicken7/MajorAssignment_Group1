package org.auctionfx.auctionbidsystemspringboot.service;

import org.auctionfx.auctionbidsystemspringboot.exception.NotEnoughMoneyException;

import java.math.BigDecimal;
import java.util.concurrent.locks.ReentrantLock;

// Tạo một class NotEnoughMoneyException để ném ra ngoại lệ khi số tiền không đủ


// Chịu trách nhiệm về việc payment (thanh toán)
public abstract class PaymentService {
    // Các chức năng nạp rút tiền cơ bản trong thanh toán
    public static void addFreezeMoney(String userId, BigDecimal amount) {
        BigDecimal newValue = UserService.getMoneyinFrozen(userId).add(amount);
        UserService.setMoneyinFrozen(userId, newValue);
    }

    public static void subFreezeMoney(String userId, BigDecimal amount) throws NotEnoughMoneyException {
        BigDecimal userFreezeMoney = UserService.getMoneyinFrozen(userId);
        if (userFreezeMoney.compareTo(amount) >= 0) {
            BigDecimal newValue = UserService.getMoneyinFrozen(userId).subtract(amount);
            UserService.setMoneyinFrozen(userId, newValue);
        } else {
            throw new NotEnoughMoneyException("Số tiền trong ví đóng băng không đủ");
        }
    }

    public static void addWalletMoney(String userId, BigDecimal amount) {
        BigDecimal newValue = UserService.getMoneyOnWallet(userId).add(amount);
        UserService.setMoneyOnWallet(userId, newValue);
    }

    public static void subWalletMoney(String userId, BigDecimal amount) throws NotEnoughMoneyException {
        BigDecimal userWalletMoney = UserService.getMoneyOnWallet(userId);
        if (userWalletMoney.compareTo(amount) >= 0) {
            BigDecimal newValue = UserService.getMoneyOnWallet(userId).subtract(amount);
            UserService.setMoneyOnWallet(userId, newValue);
        } else {
            throw new NotEnoughMoneyException("Số tiền trong ví không đủ");
        }
    }

    // ------------------------------------------------------------------------------------
    // Chức năng đóng băng tiền

    // THÊM: Khai báo ReentrantLock
    private static final ReentrantLock lock = new ReentrantLock();
    // Đóng băng số tiền
    public static void FreezeMoney(String userId, BigDecimal amount) throws NotEnoughMoneyException {
        lock.lock();
        try {
            subWalletMoney(userId, amount);
            addFreezeMoney(userId, amount);
        } catch (NotEnoughMoneyException e) {
            // Sửa trong controller sau
            System.err.println(e.getMessage());
            lock.unlock();
            throw new NotEnoughMoneyException(e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    // Dừng đóng băng tiền
    public static void unFreezeMoney(String userId, BigDecimal amount) throws NotEnoughMoneyException {
        lock.lock();
        try {
            subFreezeMoney(userId, amount);
            addWalletMoney(userId, amount);
        } catch (NotEnoughMoneyException e) {
            // Sửa trong controller sau
            System.err.println(e.getMessage());
            lock.unlock();
            throw new NotEnoughMoneyException(e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    // Chuyển tiền đóng băng (đấu giá thành công)
    public static void transferMoney(String fromUserId, String toUserId, BigDecimal amount) throws NotEnoughMoneyException {
        lock.lock();
        try {
            subFreezeMoney(fromUserId, amount);
            addWalletMoney(toUserId, amount);
        } catch (NotEnoughMoneyException e) {
            // Sửa trong controller sau
            System.err.println(e.getMessage());
            lock.unlock();
            throw new NotEnoughMoneyException(e.getMessage());
        } finally {
            lock.unlock();
        }
    }
}
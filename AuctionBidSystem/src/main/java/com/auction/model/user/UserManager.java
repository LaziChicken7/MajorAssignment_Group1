package com.auction.model.user;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.auction.model.enums.Role;
import com.auction.model.item.Item;

public class UserManager {

    static List<User> users = new ArrayList<>();
    
    // ĐĂNG NHẬP, ĐĂNG KÝ, QUÊN MẬT KHẨU
    
    public boolean Register(String userName, String password, String fullName, String email, String numberPhone, String citizenId) {

        for (User user : users) {
            if (user.userName.equals(userName)) {
                System.err.println("Tài khoản đã tồn tại");
                return false;
            }
            if (user.email.equals(email)) {
                System.err.println("Email đã tồn tại");
                return false;
            }
            if (user.numberPhone.equals(numberPhone)) {
                System.err.println("Số điện thoại đã tồn tại");
                return false;
            }
            if (user.citizenId.equals(citizenId)) {
                System.err.println("CCCD đã tồn tại");
                return false;
            }
        }
        // DÙNG FOR -> CHƯA TỐI ƯU
        
        if (!checkWeakPassword(password)) {
            System.err.println("Mật khẩu yếu");
            return false;
        }

        users.add(new Bidder(userName, password, fullName, email, numberPhone, citizenId, "", Role.BIDDER));
        
        System.err.println("Đăng ký thành công");
        return true;

        //Cần phải sửa sau khi áp dụng Database/DAO
        // if (userName.equals() && password.equals() && fullName.equals() && email.equals() && numberPhone.equals()) {
            //     System.err.println("Đăng ký thành công");
        //     return true;
        // } else {
            //     System.err.println("Đăng ký thất bại");
            //     return false;
        // }
        
    }

    // ĐĂNG NHẬP
    
    public boolean Login(String userName, String password) {
        for (User user : users) {
            if (user.userName.equals(userName) && user.password.equals(User.encode(password))) {
                System.err.println("Đăng nhập thành công");
                return true;
            }
        }

        System.out.println("Sai tài khoản hoặc mật khẩu");
        return false;

        //Cần phải sửa sau khi áp dụng Database/DAO
        // if (userName.equals(userName) && password.equals(User.encode(password))) {
        //     System.err.println("Đăng nhập thành công");
        //     return true;
        // } else {
        //     System.err.println("Sai tài khoản hoặc mật khẩu");
        //     return false;
        // }
    }

    // QUÊN MẬT KHẨU

    public boolean ForgotPassword(String userName, String email, String newPassword) {
        for (User user : users) {
            if (user.userName.equals(userName) && user.email.equals(email)) {
                user.setPassword(newPassword);
                System.err.println("Đổi mật khẩu thành công");
                return true;
            }
        }

        System.err.println("Không tìm thấy tài khoản");
        return false;

        // Cần phải sửa sau khi áp dụng Database/DAO
        // User user = new User();
        // user.updatePassword(newPassword);
        // System.err.println("Đổi mật khẩu thành công");
        // return true;
    }

    // KIỂM TRA PASSWORD YẾU

    private boolean checkWeakPassword(String password) {
        if (password.length() < 8) {
            return false;
        }
        else if (!password.matches(".*[A-Z].*")) {
            return false;
        }
        else if (!password.matches(".*[a-z].*")) {
            return false;
        }
        else if (!password.matches(".*\\d.*")) {
            return false;
        }
        return true;
    }

    //---------------------------------------------------------
    // LẤY THÔNG TIN CỦA MỘT NGƯỜI

    public User getUser(String userName) {
        for (User user : users) {
            if (user.userName.equals(userName)) {
                return user;
            }
        }
        return null;
    }

    // LẤY VÀ UPDATE DATABASE

    public static String getUserName(String userName) {
        for (User user : users) {
            if (user.userName.equals(userName)) {
                return user.userName;
            }
        }
        return null;
    }
    public static void updateUserName(String userName, String newUserName) {
        for (User user : users) {
            if (user.userName.equals(userName)) {
                user.setUserName(newUserName);
                break;
            }
        }
    }

    public static String getFullName(String userName) {
        for (User user : users) {
            if (user.userName.equals(userName)) {
                return user.fullName;
            }
        }
        return null;
    }
    public static void updateFullName(String userName, String fullName) {
        for (User user : users) {
            if (user.userName.equals(userName)) {
                user.setFullName(fullName);
                break;
            }
        }
    }

    public static String getEmail(String userName) {
        for (User user : users) {
            if (user.userName.equals(userName)) {
                return user.email;
            }
        }
        return null;
    }
    public static void updateEmail(String userName, String email) {
        for (User user : users) {
            if (user.userName.equals(userName)) {
                user.setEmail(email);
                break;
            }
        }
    }

    public static String getPassword(String userName) {
        for (User user : users) {
            if (user.userName.equals(userName)) {
                return user.password;
            }
        }
        return null;
    }
    public static void updatePassword(String userName, String password) {
        for (User user : users) {
            if (user.userName.equals(userName)) {
                user.setPassword(password);
                break;
            }
        }
    }

    public static String getNumberPhone(String userName) {
        for (User user : users) {
            if (user.userName.equals(userName)) {
                return user.numberPhone;
            }
        }
        return null;
    }
    public static void updateNumberPhone(String userName, String numberPhone) {
        for (User user : users) {
            if (user.userName.equals(userName)) {
                user.setNumberPhone(numberPhone);
                break;
            }
        }
    }

    public static String getCitizenId(String userName) {
        for (User user : users) {
            if (user.userName.equals(userName)) {
                return user.citizenId;
            }
        }
        return null;
    }

    public static void setMoneyinFrozen(String userName, BigDecimal moneyinFrozen) {
        for (User user : users) {
            if (user.userName.equals(userName)) {
                ((Bidder) user).setMoneyinFrozen(moneyinFrozen);
                break;
            }
        }
    }
    public static BigDecimal getMoneyinFrozen(String userName) {
        for (User user : users) {
            if (user.userName.equals(userName)) {
                return ((Bidder) user).getMoneyinFrozen();
            }
        }
        return BigDecimal.ZERO;
    }

    public static void setMoneyOnWallet(String userName, BigDecimal moneyinWallet) {
        for (User user : users) {
            if (user.userName.equals(userName)) {
                ((Bidder) user).setMoneyOnWallet(moneyinWallet);
                break;
            }
        }
    }
    public static BigDecimal getMoneyOnWallet(String userName) {
        for (User user : users) {
            if (user.userName.equals(userName)) {
                return ((Bidder) user).getMoneyOnWallet();
            }
        }
        return BigDecimal.ZERO;
    }

    public static void setSuccessBidItem(String userName, Item item) {
        for (User user : users) {
            if (user.userName.equals(userName)) {
                ((Bidder) user).addSuccessBidItem(item);
                break;
            }
        }
    }

    public static void setFailedBidItem(String userName, Item item) {
        for (User user : users) {
            if (user.userName.equals(userName)) {
                ((Bidder) user).addFailedBidItem(item);
                break;
            }
        }
    }


}

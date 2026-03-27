package com.auction.model;

import java.util.ArrayList;
import java.util.List;

public class UserManager {

    List<User> users = new ArrayList<>();
    
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
        
        if (!checkWeakPassword(password)) {
            System.err.println("Mật khẩu yếu");
            return false;
        }
        
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
                user.updatePassword(newPassword);
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
}

package com.auction.model;

public class UserManager {
    //ĐĂNG NHẬP, ĐĂNG KÝ, QUÊN MẬT KHẨU

    public boolean Register(String userName, String password, String fullName, String email, String numberPhone, String citizenId) {
        //Cần phải sửa sau khi áp dụng Database/DAO
        // if (userName.equals() && password.equals() && fullName.equals() && email.equals() && numberPhone.equals()) {
        //     System.err.println("Đăng ký thành công");
        //     return true;
        // } else {
        //     System.err.println("Đăng ký thất bại");
        //     return false;
        // }
        return true;
    }

    public boolean Login(String userName, String password) {
        //Cần phải sửa sau khi áp dụng Database/DAO
        // if (userName.equals(userName) && password.equals(User.encode(password))) {
        //     System.err.println("Đăng nhập thành công");
        //     return true;
        // } else {
        //     System.err.println("Sai tài khoản hoặc mật khẩu");
        //     return false;
        // }
        return true;
    }

    public boolean ForgotPassword(String userName, String newPassword) {
        //Cần phải sửa sau khi áp dụng Database/DAO
        // User user = new User();
        // user.updatePassword(newPassword);
        // System.err.println("Đổi mật khẩu thành công");
        // return true;
        return true;
    }
}

package com.auction.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;

public class RegisterController {

    @FXML private TextField txtFullName, txtEmail, txtUsername;
    @FXML private PasswordField txtPassword, txtConfirmPassword;

    // Xử lý khi bấm nút Đăng ký
    @FXML
    private void handleRegister() {
        String fullName = txtFullName.getText();
        String user = txtUsername.getText();
        String pass = txtPassword.getText();
        String confirmPass = txtConfirmPassword.getText();

        // Kiểm tra cơ bản
        if (fullName.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            System.out.println("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (!pass.equals(confirmPass)) {
            System.out.println("Mật khẩu xác nhận không khớp!");
            return;
        }

        // Logic lưu user vào Database hoặc File tại đây
        System.out.println("Đăng ký thành công cho: " + user);

        // Đăng ký xong tự quay về trang Login
        goToLogin();
    }

    // Chuyển về trang Login
    @FXML
    private void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/auction/view/Login.fxml"));
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
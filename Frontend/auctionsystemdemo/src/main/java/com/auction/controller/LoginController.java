package com.auction.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    @FXML
    private void handleLogin() {
        String user = txtUsername.getText();
        String pass = txtPassword.getText();

        // Kiểm tra đăng nhập giả định (Bạn có thể sửa thành gọi Database)
        if (user.equals("admin") && pass.equals("123")) {
            System.out.println("Đăng nhập thành công!");
            switchToMain();
        } else {
            System.out.println("Sai tài khoản hoặc mật khẩu!");
        }

    }

    private void switchToMain() {
        try {
            // Sau khi đăng nhập xong, chuyển sang file Main.fxml (có Sidebar)
            Parent root = FXMLLoader.load(getClass().getResource("/com/auction/view/Main.fxml"));
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Trong LoginController.java thêm hàm này:
    @FXML
    private void goToRegister() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/auction/view/Register.fxml"));
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
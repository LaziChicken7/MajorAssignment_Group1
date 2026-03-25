package com.auction.controller;

// public package com.auction.controller;

import java.io.IOException;

import com.auction.App;
import com.auction.model.User;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;

    @FXML
    private void handleLogin() throws IOException {
        String user = txtUsername.getText();
        String pass = txtPassword.getText();

        // Dummy authentication (Sau này bạn kết nối Database/DAO ở đây)
        if (user.equals("admin") && pass.equals("123456")) {
            // Giả lập tạo đối tượng User sau khi login thành công
            User loggedInUser = new User(user, "Admin");
            
            // Chuyển sang trang chủ
            App.setRoot("view/Home");
        } else {
            lblError.setText("Sai tên đăng nhập hoặc mật khẩu!");
        }
    }
}

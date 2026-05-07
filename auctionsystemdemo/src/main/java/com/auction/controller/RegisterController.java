package com.auction.controller;

import com.auction.model.ApiResponse;
import com.auction.model.UserCreationRequest;
import com.auction.util.ApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController {

    // Thêm 2 trường mới là txtPhoneNumber và txtCitizenId
    @FXML private TextField txtFullName, txtEmail, txtUsername, txtPhoneNumber, txtCitizenId;
    @FXML private PasswordField txtPassword, txtConfirmPassword;

    @FXML
    private void handleRegister() {
        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        String user = txtUsername.getText().trim();
        String phone = txtPhoneNumber.getText().trim();     // Lấy Số điện thoại
        String citizenId = txtCitizenId.getText().trim();   // Lấy CCCD
        String pass = txtPassword.getText();
        String confirmPass = txtConfirmPassword.getText();

        // Cập nhật điều kiện kiểm tra rỗng
        if (fullName.isEmpty() || user.isEmpty() || pass.isEmpty() || email.isEmpty() || phone.isEmpty() || citizenId.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (!pass.equals(confirmPass)) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Mật khẩu xác nhận không khớp!");
            return;
        }

        // Đóng gói dữ liệu gửi lên Spring Boot
        UserCreationRequest request = new UserCreationRequest();
        request.fullName = fullName;
        request.email = email;
        request.userName = user;
        request.numberPhone = phone;       // Gắn vào request
        request.citizenId = citizenId;     // Gắn vào request
        request.password = pass;
        request.role = "BIDDER";           // Mặc định tạo tài khoản là BIDDER

        // Gọi API của Spring Boot
        ApiService.postAsync("/users/register", request)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        // Nếu thành công (mã 2xx)
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            ApiResponse apiResponse = ApiService.gson.fromJson(response.body(), ApiResponse.class);
                            if (apiResponse.code == 1000) {
                                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký thành công tài khoản: " + user);
                                goToLogin();
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Lỗi từ Backend", apiResponse.message);
                            }
                        }
                        // Nếu thất bại (400, 500...)
                        else {
                            try {
                                ApiResponse errResponse = ApiService.gson.fromJson(response.body(), ApiResponse.class);
                                showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", errResponse.message);
                            } catch (Exception e) {
                                System.out.println("Lỗi thô từ Server: " + response.body());
                                showAlert(Alert.AlertType.ERROR, "Lỗi kết nối (Mã " + response.statusCode() + ")", "Chi tiết lỗi:\n" + response.body());
                            }
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Mất kết nối", "Không thể kết nối đến máy chủ Spring Boot ở địa chỉ:\n" + ApiService.BASE_URL));
                    return null;
                });
    }

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

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);

        if (content != null && content.length() > 100) {
            javafx.scene.control.TextArea textArea = new javafx.scene.control.TextArea(content);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);

            alert.getDialogPane().setContent(textArea);
            alert.getDialogPane().setPrefSize(600, 400);
        } else {
            alert.setContentText(content);
        }

        alert.showAndWait();
    }
}
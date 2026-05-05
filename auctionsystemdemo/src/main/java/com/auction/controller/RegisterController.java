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

    @FXML private TextField txtFullName, txtEmail, txtUsername;
    @FXML private PasswordField txtPassword, txtConfirmPassword;

    @FXML
    private void handleRegister() {
        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        String user = txtUsername.getText().trim();
        String pass = txtPassword.getText();
        String confirmPass = txtConfirmPassword.getText();

        if (fullName.isEmpty() || user.isEmpty() || pass.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (!pass.equals(confirmPass)) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Mật khẩu xác nhận không khớp!");
            return;
        }

        // Đóng gói dữ liệu
        UserCreationRequest request = new UserCreationRequest();
        request.fullName = fullName;
        request.email = email;
        request.userName = user;
        request.password = pass;
        request.role = "BIDDER"; // Mặc định ai tạo nick cũng là người mua

        // Gọi API của Spring Boot
        ApiService.postAsync("/users/register", request)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        // Nếu thành công (mã 2xx)
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            ApiResponse apiResponse = ApiService.gson.fromJson(response.body(), ApiResponse.class);
                            if (apiResponse.code == 1000) {
                                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký thành công!");
                                goToLogin();
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Lỗi từ Backend", apiResponse.message);
                            }
                        }
                        // Nếu thất bại (404, 400, 500...)
                        else {
                            try {
                                // Cố gắng đọc lỗi dạng JSON chuẩn của mình trước
                                ApiResponse errResponse = ApiService.gson.fromJson(response.body(), ApiResponse.class);
                                showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", errResponse.message);
                            } catch (Exception e) {
                                // NẾU KHÔNG PHẢI JSON CHUẨN (Ví dụ 404 Not Found), IN THẲNG RAW TEXT RA MÀN HÌNH
                                System.out.println("Lỗi thô từ Server: " + response.body()); // In ra console để debug
                                showAlert(Alert.AlertType.ERROR, "Lỗi kết nối (Mã " + response.statusCode() + ")", "Chi tiết lỗi:\n" + response.body());
                            }
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Mất kết nối", "Không thể kết nối đến máy chủ!"));
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

        // Nếu nội dung lỗi quá dài (như mã HTML), cho nó vào một khung TextArea to để dễ cuộn và copy
        if (content != null && content.length() > 100) {
            javafx.scene.control.TextArea textArea = new javafx.scene.control.TextArea(content);
            textArea.setEditable(false); // Không cho sửa
            textArea.setWrapText(true);  // Tự động xuống dòng
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);

            alert.getDialogPane().setContent(textArea);
            alert.getDialogPane().setPrefSize(600, 400); // Kích thước to ra (Rộng 600, Cao 400)
        } else {
            alert.setContentText(content);
        }

        alert.showAndWait();
    }
}
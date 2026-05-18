package com.auction.controller.home;

import com.auction.model.ApiResponse;
import com.auction.model.UserCreationRequest;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class RegisterController {

    @FXML private HBox rootPane;
    @FXML private TextField txtFullName, txtEmail, txtUsername, txtPhoneNumber, txtCitizenId;
    @FXML private PasswordField txtPassword, txtConfirmPassword;

    // --- DARK MODE SWITCH ---
    @FXML private Rectangle switchBackground;
    @FXML private Circle switchKnob;

    @FXML
    public void initialize() {
        applyCurrentTheme(false);
    }

    @FXML
    public void handleThemeToggle() {
        SessionManager.isDarkMode = !SessionManager.isDarkMode;
        applyCurrentTheme(true);
    }

    private void applyCurrentTheme(boolean animate) {
        // 1. Cập nhật màu nền nút gạt và CSS Theme
        if (SessionManager.isDarkMode) {
            switchBackground.setFill(Color.web("#2c3e50"));
            if (!rootPane.getStyleClass().contains("dark-theme")) {
                rootPane.getStyleClass().add("dark-theme");
            }
        } else {
            switchBackground.setFill(Color.web("#bdc3c7"));
            rootPane.getStyleClass().remove("dark-theme");
        }

        // 2. Xác định vị trí của nút tròn
        // (Trong FXML của bạn đang để mặc định translateX="-16", nên ta dùng 16 và -16 để cân xứng)
        double targetX = SessionManager.isDarkMode ? 16 : -16;

        // 3. Xử lý di chuyển Knob
        if (animate) {
            // Khi người dùng click gạt: dùng Animation mượt mà
            TranslateTransition transition = new TranslateTransition(Duration.millis(250), switchKnob);
            transition.setToX(targetX);
            transition.play();
        } else {
            // Khi mới vào trang (hàm initialize gọi): Gán giá trị ngay lập tức, bỏ qua animation
            switchKnob.setTranslateX(targetX);
        }
    }

    @FXML
    private void handleRegister() {
        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        String user = txtUsername.getText().trim();
        String phone = txtPhoneNumber.getText().trim();
        String citizenId = txtCitizenId.getText().trim();
        String pass = txtPassword.getText();
        String confirmPass = txtConfirmPassword.getText();

        if (fullName.isEmpty() || user.isEmpty() || pass.isEmpty() || email.isEmpty() || phone.isEmpty() || citizenId.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (!pass.equals(confirmPass)) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Mật khẩu xác nhận không khớp!");
            return;
        }

        UserCreationRequest request = new UserCreationRequest();
        request.fullName = fullName;
        request.email = email;
        request.userName = user;
        request.numberPhone = phone;
        request.citizenId = citizenId;
        request.password = pass;
        request.role = "BIDDER";

        ApiService.postAsync("/users/register", request).thenAccept(response -> {
            Platform.runLater(() -> {
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    ApiResponse apiResponse = ApiService.gson.fromJson(response.body(), ApiResponse.class);
                    if (apiResponse.code == 1000) {
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký thành công tài khoản: " + user);
                        goToLogin();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi từ Backend", apiResponse.message);
                    }
                } else {
                    try {
                        ApiResponse errResponse = ApiService.gson.fromJson(response.body(), ApiResponse.class);
                        showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", errResponse.message);
                    } catch (Exception e) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi kết nối (Mã " + response.statusCode() + ")", "Chi tiết lỗi:\n" + response.body());
                    }
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Mất kết nối", "Không thể kết nối đến máy chủ:\n" + ApiService.BASE_URL));
            return null;
        });
    }

    @FXML
    private void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/auction/view/dashboard/Login.fxml"));
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title); alert.setHeaderText(null);
        if (content != null && content.length() > 100) {
            javafx.scene.control.TextArea textArea = new javafx.scene.control.TextArea(content);
            textArea.setEditable(false); textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE); textArea.setMaxHeight(Double.MAX_VALUE);
            alert.getDialogPane().setContent(textArea);
            alert.getDialogPane().setPrefSize(600, 400);
        } else {
            alert.setContentText(content);
        }
        com.auction.util.AlertUtils.applyStyle(alert);
        alert.showAndWait();
    }
}
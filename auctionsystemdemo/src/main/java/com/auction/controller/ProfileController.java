package com.auction.controller;

import com.auction.model.ApiResponse;
import com.auction.model.UserProfile;
import com.auction.model.UserUpdateRequest;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class ProfileController {

    @FXML private TextField txtUsername;
    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextField txtCitizenId;
    @FXML private PasswordField txtPassword; // Nơi nhập mật khẩu mới

    @FXML
    public void initialize() {
        // Tự động lấy dữ liệu từ Spring Boot khi vừa mở màn hình Profile
        loadUserData();
    }

    // ==========================================
    // 1. HÀM LẤY DỮ LIỆU TỪ SERVER ĐỔ VÀO UI
    // ==========================================
    private void loadUserData() {
        String currentUser = SessionManager.userName;
        if (currentUser == null) return;

        // Gọi API GET lấy thông tin profile
        ApiService.getAsync("/users/profile/" + currentUser)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            ApiResponse apiResponse = ApiService.gson.fromJson(response.body(), ApiResponse.class);
                            if (apiResponse.code == 1000) {
                                // Ép kiểu Json object thành class UserProfile
                                UserProfile profile = ApiService.gson.fromJson(apiResponse.result, UserProfile.class);

                                // Đổ dữ liệu lên các ô text trên màn hình
                                txtUsername.setText(profile.userName);
                                txtFullName.setText(profile.fullName);
                                txtEmail.setText(profile.email);
                                txtPhone.setText(profile.numberPhone);
                                txtCitizenId.setText(profile.citizenId);
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Lỗi tải dữ liệu", apiResponse.message);
                            }
                        } else {
                            System.out.println("Lỗi load dữ liệu: " + response.body());
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ."));
                    return null;
                });
    }

    // ==========================================
    // 2. HÀM LƯU THÔNG TIN (UPDATE PROFILE)
    // ==========================================
    @FXML
    public void handleUpdateProfile(ActionEvent event) {
        String currentUser = SessionManager.userName;
        if (currentUser == null) return;

        // Lấy dữ liệu từ giao diện
        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();
        String newPassword = txtPassword.getText();

        // Kiểm tra điều kiện cơ bản
        if (fullName.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Họ tên và Email không được để trống!");
            return;
        }

        // Đóng gói dữ liệu gửi lên (Gán vào Model)
        UserUpdateRequest request = new UserUpdateRequest();
        request.fullName = fullName;
        request.email = email;
        request.numberPhone = phone;

        // Nếu ô password để trống -> Gửi null (Backend sẽ hiểu là không đổi pass)
        // Nếu gửi chuỗi rỗng "", Spring Boot sẽ báo lỗi @Size(min=8)
        request.password = newPassword.isEmpty() ? null : newPassword;

        // Gọi API PUT để cập nhật
        ApiService.putAsync("/users/profile/" + currentUser, request)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            ApiResponse apiResponse = ApiService.gson.fromJson(response.body(), ApiResponse.class);
                            if (apiResponse.code == 1000) {
                                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Cập nhật thông tin thành công!");

                                // Cập nhật lại tên vào SessionManager (để các màn hình khác cập nhật theo)
                                SessionManager.fullName = fullName;

                                // Xóa trắng ô password sau khi đổi thành công
                                txtPassword.clear();
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Lỗi cập nhật", apiResponse.message);
                            }
                        } else {
                            // Bắt lỗi Validation (ví dụ trùng Email, Pass ngắn...)
                            try {
                                ApiResponse errResponse = ApiService.gson.fromJson(response.body(), ApiResponse.class);
                                showAlert(Alert.AlertType.ERROR, "Lỗi xác thực", errResponse.message);
                            } catch (Exception e) {
                                showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Mã lỗi: " + response.statusCode());
                            }
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Mất kết nối", "Không thể gọi tới máy chủ."));
                    return null;
                });
    }

    // ==========================================
    // 3. HÀM ĐĂNG XUẤT (QUAY VỀ TRANG LOGIN)
    // ==========================================
    @FXML
    public void handleLogout(ActionEvent event) {
        // 1. Xóa thông tin người dùng khỏi bộ nhớ tạm
        SessionManager.logout();

        // 2. Chuyển scene (Thay thế hoàn toàn cửa sổ Main bằng cửa sổ Login)
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/auction/view/Login.fxml"));

            // Lấy Stage (Cửa sổ hiện tại) từ event của nút bấm
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi giao diện", "Không thể mở trang đăng nhập!");
        }
    }

    // ==========================================
    // 4. HÀM TIỆN ÍCH (HIỂN THỊ THÔNG BÁO)
    // ==========================================
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
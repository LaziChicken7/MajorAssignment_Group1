package com.auction.controller.home;

import com.auction.model.ApiResponse;
import com.auction.model.LoginRequest;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
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

public class LoginController {

    @FXML private TextField txtServerIp;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    @FXML
    public void initialize() {
        txtServerIp.setText(ApiService.BASE_URL);
    }

    @FXML
    private void handleApplyIp() {
        String serverIp = txtServerIp.getText().trim();
        if (!serverIp.isEmpty()) {
            if (serverIp.endsWith("/")) {
                serverIp = serverIp.substring(0, serverIp.length() - 1);
            }
            ApiService.BASE_URL = serverIp;
            showAlert(Alert.AlertType.INFORMATION, "Cấu hình thành công", "Hệ thống sẽ kết nối đến IP:\n" + ApiService.BASE_URL);
        } else {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập địa chỉ IP của Server!");
        }
    }

    @FXML
    private void handleLogin() {
        String serverIp = txtServerIp.getText().trim();
        if (!serverIp.isEmpty()) {
            if (serverIp.endsWith("/")) {
                serverIp = serverIp.substring(0, serverIp.length() - 1);
            }
            ApiService.BASE_URL = serverIp;
        }

        String user = txtUsername.getText().trim();
        String pass = txtPassword.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập tài khoản và mật khẩu!");
            return;
        }

        LoginRequest request = new LoginRequest(user, pass);

        ApiService.postAsync("/users/login", request)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            ApiResponse apiResponse = ApiService.gson.fromJson(response.body(), ApiResponse.class);
                            if (apiResponse.code == 1000) {
                                var resultObj = apiResponse.result.getAsJsonObject();
                                SessionManager.userName = resultObj.get("userName").getAsString();
                                SessionManager.fullName = resultObj.get("fullName") != null && !resultObj.get("fullName").isJsonNull() ? resultObj.get("fullName").getAsString() : "Người dùng";
                                SessionManager.role = resultObj.get("role").getAsString();

                                System.out.println("Đăng nhập thành công: " + SessionManager.userName);
                                switchToMain();
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Lỗi đăng nhập", apiResponse.message);
                            }
                        } else {
                            try {
                                ApiResponse errResponse = ApiService.gson.fromJson(response.body(), ApiResponse.class);
                                if (errResponse.code == 4006) {
                                    showAlert(Alert.AlertType.ERROR, "TÀI KHOẢN BỊ KHÓA", errResponse.message);
                                } else {
                                    showAlert(Alert.AlertType.ERROR, "Lỗi đăng nhập", errResponse.message);
                                }
                            } catch (Exception e) {
                                showAlert(Alert.AlertType.ERROR, "Lỗi", "Tài khoản hoặc mật khẩu không chính xác!");
                            }
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Mất kết nối", "Không thể kết nối đến Server:\n" + ApiService.BASE_URL));
                    return null;
                });
    }

    private void switchToMain() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/auction/view/dashboard/Main.fxml"));
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToRegister() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/auction/view/dashboard/Register.fxml"));
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
        alert.setContentText(content);
        alert.showAndWait();
    }
}
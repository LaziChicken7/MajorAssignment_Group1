package com.auction.controller;

import com.auction.model.ApiResponse;
import com.auction.model.PaymentRequest;
import com.auction.model.WalletDataResponse;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import java.io.IOException;

public class DepositController {

    @FXML private TextField amountField; // Chỉ có ở màn hình nhập tiền
    @FXML private Label lblBankAccount;  // Có ở cả màn hình nhập và màn hình thành công
    @FXML private TextField balanceField;
    @FXML private Label lblFrozenBalance;

    @FXML
    public void initialize() {
        // Tự động load dữ liệu thực tế lên thẻ VISA khi mở trang
        loadRealData();
    }

    private void loadRealData() {
        if (SessionManager.userName == null || lblBankAccount == null) return;

        ApiService.getAsync("/payments/" + SessionManager.userName + "/history")
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            ApiResponse apiRes = ApiService.gson.fromJson(response.body(), ApiResponse.class);
                            if (apiRes.code == 1000) {
                                WalletDataResponse data = ApiService.gson.fromJson(apiRes.result, WalletDataResponse.class);
                                lblBankAccount.setText(data.bankAccountNumber != null ? data.bankAccountNumber : "Chưa có");
                                if (balanceField != null) balanceField.setText(formatMoney(data.moneyOnWallet));
                                if (lblFrozenBalance != null) lblFrozenBalance.setText(formatMoney(data.moneyinFrozen));
                            }
                        }
                    });
                });
    }

    @FXML
    public void handleConfirm() {
        if (amountField == null) return;
        String amountStr = amountField.getText().replace(".", "").replace(",", "").trim();
        if (amountStr.isEmpty()) { showAlert("Vui lòng nhập số tiền!"); return; }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) { showAlert("Số tiền phải lớn hơn 0!"); return; }

            PaymentRequest request = new PaymentRequest(SessionManager.userName, amount);

            ApiService.postAsync("/payments/deposit", request).thenAccept(response -> {
                Platform.runLater(() -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        ApiResponse apiResponse = ApiService.gson.fromJson(response.body(), ApiResponse.class);
                        if (apiResponse.code == 1000) {
                            switchView("/com/auction/view/DepositSuccess.fxml");
                        } else {
                            showAlert("Lỗi nạp tiền: " + apiResponse.message);
                        }
                    } else {
                        showAlert("Nạp tiền thất bại! Mã lỗi: " + response.statusCode());
                    }
                });
            }).exceptionally(ex -> {
                Platform.runLater(() -> showAlert("Mất kết nối đến máy chủ!"));
                return null;
            });
        } catch (NumberFormatException e) {
            showAlert("Số tiền không hợp lệ! Vui lòng chỉ nhập số.");
        }
    }

    // Hàm chuyển trang không dùng event nữa để chống lỗi Crash
    private void switchView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();

            if (lblBankAccount != null && lblBankAccount.getScene() != null) {
                Pane contentArea = (Pane) lblBankAccount.getScene().lookup("#contentArea");
                if (contentArea != null) {
                    contentArea.getChildren().setAll(view);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleBackToWallet() {
        switchView("/com/auction/view/Wallet.fxml");
    }

    @FXML
    public void handleCancel() {
        handleBackToWallet();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String formatMoney(double amount) {
        return String.format("%,.0f", amount).replace(",", ".") + " VND";
    }
}
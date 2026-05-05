package com.auction.controller;

import com.auction.model.ApiResponse;
import com.auction.model.TransactionHistoryResponse;
import com.auction.model.WalletDataResponse;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

public class WalletController {

    @FXML private VBox walletRoot;
    @FXML private VBox historySection;

    // Các phần tử hiển thị Số dư
    @FXML private TextField balanceField;
    @FXML private Label eyeIconText;
    @FXML private Label lblFrozenBalance;

    // Các phần tử hiển thị Lịch sử giao dịch
    @FXML private VBox vboxSuccess;
    @FXML private VBox vboxFailed;

    @FXML private Label lblBankAccount; // Gắn ID ở đây

    private double actualBalance = 0.0;
    private boolean isBalanceVisible = false;

    @FXML
    public void initialize() {
        // Gọi API lấy dữ liệu ngay khi load trang
        loadWalletData();
    }

    private void loadWalletData() {
        String userName = SessionManager.userName;
        if (userName == null) return;

        ApiService.getAsync("/payments/" + userName + "/history")
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            ApiResponse apiResponse = ApiService.gson.fromJson(response.body(), ApiResponse.class);
                            if (apiResponse.code == 1000) {
                                WalletDataResponse walletData = ApiService.gson.fromJson(apiResponse.result, WalletDataResponse.class);

                                // HIỂN THỊ SỐ TÀI KHOẢN
                                lblBankAccount.setText(walletData.bankAccountNumber != null ? walletData.bankAccountNumber : "Chưa có");

                                // 1. Cập nhật số dư ví chính
                                this.actualBalance = walletData.moneyOnWallet;
                                updateBalanceDisplay();

                                // 2. Cập nhật số dư đóng băng
                                lblFrozenBalance.setText(formatMoney(walletData.moneyinFrozen));

                                // 3. Đổ dữ liệu lịch sử giao dịch (Hiển thị tối đa 3 giao dịch mới nhất)
                                renderTransactions(walletData.successTransaction, vboxSuccess, "#00C853");
                                renderTransactions(walletData.failedTransaction, vboxFailed, "#FF0000");
                            }
                        } else {
                            System.out.println("Lỗi load ví tiền: " + response.body());
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> System.out.println("Lỗi kết nối máy chủ!"));
                    return null;
                });
    }

    // ==========================================
    // LOGIC ẨN/HIỆN SỐ DƯ
    // ==========================================
    @FXML
    public void toggleBalanceVisibility(MouseEvent event) {
        isBalanceVisible = !isBalanceVisible;
        updateBalanceDisplay();
    }

    private void updateBalanceDisplay() {
        if (isBalanceVisible) {
            balanceField.setText(formatMoney(actualBalance));
            eyeIconText.setText("Ẩn");
            eyeIconText.setStyle("-fx-font-size: 16px; -fx-text-fill: #FF0000; -fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            balanceField.setText("****** VND");
            eyeIconText.setText("Hiện");
            eyeIconText.setStyle("-fx-font-size: 16px; -fx-text-fill: #0A439D; -fx-font-weight: bold; -fx-cursor: hand;");
        }
    }

    // ==========================================
    // HÀM TẠO GIAO DIỆN LỊCH SỬ GIAO DỊCH
    // ==========================================
    private void renderTransactions(List<TransactionHistoryResponse> transactions, VBox container, String colorCode) {
        container.getChildren().clear(); // Xóa dữ liệu cũ

        if (transactions == null || transactions.isEmpty()) {
            Label lblEmpty = new Label("Chưa có giao dịch nào");
            lblEmpty.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
            container.getChildren().add(lblEmpty);
            return;
        }

        // Chỉ hiển thị 3 cái mới nhất cho gọn màn hình
        int limit = Math.min(transactions.size(), 3);
        for (int i = 0; i < limit; i++) {
            TransactionHistoryResponse tx = transactions.get(i);

            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #F8F9FB; -fx-background-radius: 25; -fx-padding: 10 20 10 20;");

            Label lblName = new Label(tx.itemName);
            lblName.setStyle("-fx-font-size: 14px;");
            HBox.setHgrow(lblName, Priority.ALWAYS);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label lblAmount = new Label(formatMoney(tx.amount));
            lblAmount.setStyle("-fx-background-color: " + colorCode + "; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 15; -fx-font-weight: bold;");

            row.getChildren().addAll(lblName, spacer, lblAmount);
            container.getChildren().add(row);
        }
    }

    // ==========================================
    // CÁC NÚT ĐIỀU HƯỚNG MÀN HÌNH (RÚT/NẠP)
    // ==========================================
    // Thêm hàm hỗ trợ chuyển đổi toàn màn hình (thay vì nhét xuống dưới)
    private void switchView(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            javafx.scene.Scene scene = ((Node) event.getSource()).getScene();
            javafx.scene.layout.Pane contentArea = (javafx.scene.layout.Pane) scene.lookup("#contentArea");

            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleDeposit(ActionEvent event) {
        // Thay vì nhét vào historySection, ta chuyển hẳn sang trang Nạp tiền
        switchView(event, "/com/auction/view/DepositForm.fxml");
    }

    @FXML
    public void handleWithdraw(ActionEvent event) {
        switchView(event, "/com/auction/view/WithDrawForm.fxml");
    }

    @FXML
    public void handleSuccessfulTransaction(ActionEvent event) {
        try {
            Node successView = FXMLLoader.load(getClass().getResource("/com/auction/view/SuccessfulTransaction.fxml"));
            walletRoot.getChildren().setAll(successView);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    public void handleFailureTransaction(ActionEvent event) {
        try {
            Node failView = FXMLLoader.load(getClass().getResource("/com/auction/view/FailureTransaction.fxml"));
            walletRoot.getChildren().setAll(failView);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // Hàm tiện ích: Định dạng số tiền (VD: 1700000 -> 1.700.000 VND)
    private String formatMoney(double amount) {
        return String.format("%,.0f", amount).replace(",", ".") + " VND";
    }
}
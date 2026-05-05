package com.auction.controller;

import com.auction.model.ApiResponse;
import com.auction.model.TransactionHistoryResponse;
import com.auction.model.WalletDataResponse;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TransactionController {

    // 2 Container để chứa dữ liệu
    @FXML private VBox successContainer;
    @FXML private VBox failedContainer;

    @FXML
    public void initialize() {
        // Gọi API khi vừa mở trang
        loadTransactionData();
    }

    private void loadTransactionData() {
        String userName = SessionManager.userName;
        if (userName == null) return;

        ApiService.getAsync("/payments/" + userName + "/history")
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            ApiResponse apiResponse = ApiService.gson.fromJson(response.body(), ApiResponse.class);
                            if (apiResponse.code == 1000) {
                                WalletDataResponse walletData = ApiService.gson.fromJson(apiResponse.result, WalletDataResponse.class);

                                // Nếu đang mở trang Thành công
                                if (successContainer != null) {
                                    renderList(walletData.successTransaction, successContainer, true);
                                }
                                // Nếu đang mở trang Thất bại
                                if (failedContainer != null) {
                                    renderList(walletData.failedTransaction, failedContainer, false);
                                }
                            }
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> System.out.println("Lỗi kết nối máy chủ!"));
                    return null;
                });
    }

    // Hàm dùng Java để vẽ UI (thay cho FXML)
    private void renderList(List<TransactionHistoryResponse> list, VBox container, boolean isSuccess) {
        container.getChildren().clear();

        if (list == null || list.isEmpty()) {
            Label emptyLbl = new Label("Không có giao dịch nào.");
            emptyLbl.setStyle("-fx-font-size: 16px; -fx-text-fill: #888; -fx-font-style: italic;");
            container.getChildren().add(emptyLbl);
            return;
        }

        String colorHex = isSuccess ? "#00C853" : "#FF0000";
        String statusText = isSuccess ? "Giao dịch thành công" : "Giao dịch không thành công";

        for (int i = 0; i < list.size(); i++) {
            TransactionHistoryResponse tx = list.get(i);

            // 1. Khung ngoài cùng của 1 item
            VBox card = new VBox(15);
            card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 15; -fx-padding: 20;");

            // 2. Dòng 1: Tên sản phẩm & Giá tiền
            HBox headerBox = new HBox(15);
            headerBox.setAlignment(Pos.CENTER_LEFT);

            Label lblId = new Label("SP0" + (i + 1)); // Tạm tạo ID ảo vì API chưa trả về ID
            lblId.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #2c3e50;");

            Label lblName = new Label(tx.itemName);
            lblName.setStyle("-fx-font-size: 18px; -fx-text-fill: #333333;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label lblAmount = new Label(formatMoney(tx.amount));
            lblAmount.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 15px;");

            headerBox.getChildren().addAll(lblId, lblName, spacer, lblAmount);

            // 3. Dòng 2: Hình ảnh & Chi tiết
            HBox detailBox = new HBox(20);
            detailBox.setAlignment(Pos.CENTER_LEFT);

            // Khung ảnh trống
            VBox imagePlaceholder = new VBox();
            imagePlaceholder.setPrefSize(100, 90);
            imagePlaceholder.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-border-color: #ddd; -fx-border-radius: 10;");

            // Cột thông tin chữ
            VBox infoColumn = new VBox(8);
            infoColumn.setStyle("-fx-font-size: 15px;");

            infoColumn.getChildren().addAll(
                    createDetailRow("Ngày ghi nhận:", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))),
                    createDetailRow("Địa chỉ giao hàng:", "Chưa cập nhật (Lấy từ User Profile)"),
                    createDetailRow("Trạng thái:", statusText, colorHex)
            );

            detailBox.getChildren().addAll(imagePlaceholder, infoColumn);

            // Ép tất cả vào card
            card.getChildren().addAll(headerBox, detailBox);
            container.getChildren().add(card);
        }
    }

    // Hàm hỗ trợ tạo 1 dòng (Label: Label)
    private HBox createDetailRow(String title, String value) {
        return createDetailRow(title, value, "#333333");
    }

    private HBox createDetailRow(String title, String value, String valueColor) {
        HBox row = new HBox(10);
        Label lblTitle = new Label(title);
        lblTitle.setPrefWidth(200);
        lblTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333;");

        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-text-fill: " + valueColor + "; " + (valueColor.equals("#333333") ? "" : "-fx-font-weight: bold;"));

        row.getChildren().addAll(lblTitle, lblValue);
        return row;
    }

    private String formatMoney(double amount) {
        return String.format("%,.0f", amount).replace(",", ".") + " VND";
    }

    @FXML
    public void handleBackToWallet(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/Wallet.fxml"));
            Node walletView = loader.load();
            Node source = (Node) event.getSource();
            Pane mainDisplay = (Pane) source.getScene().lookup("#contentArea");

            if (mainDisplay != null) {
                mainDisplay.getChildren().setAll(walletView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
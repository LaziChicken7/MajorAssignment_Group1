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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TransactionController {

    @FXML private VBox successContainer;
    @FXML private VBox failedContainer;

    @FXML
    public void initialize() {
        loadTransactionData();
    }

    private void loadTransactionData() {
        if (SessionManager.userName == null) return;

        ApiService.getAsync("/payments/" + SessionManager.userName + "/history").thenAccept(response -> {
            Platform.runLater(() -> {
                try {
                    if (response.statusCode() == 200) {
                        ApiResponse apiResponse = ApiService.gson.fromJson(response.body(), ApiResponse.class);
                        if (apiResponse.code == 1000) {
                            WalletDataResponse walletData = ApiService.gson.fromJson(apiResponse.result, WalletDataResponse.class);

                            if (successContainer != null) {
                                renderList(walletData.successTransaction, successContainer);
                            }
                            if (failedContainer != null) {
                                renderList(walletData.failedTransaction, failedContainer);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Lỗi parse JSON trong Transaction: " + e.getMessage());
                }
            });
        });
    }

    private void renderList(List<TransactionHistoryResponse> list, VBox container) {
        container.getChildren().clear();

        if (list == null || list.isEmpty()) {
            Label emptyLbl = new Label("Không có giao dịch nào.");
            emptyLbl.setStyle("-fx-font-size: 16px; -fx-text-fill: #888; -fx-font-style: italic;");
            container.getChildren().add(emptyLbl);
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            TransactionHistoryResponse tx = list.get(i);

            // Chống Null
            String status = tx.status != null ? tx.status : "FAILED";
            String itemName = tx.itemName != null ? tx.itemName : "Sản phẩm ẩn";

            String colorHex;
            String statusText;

            if ("SUCCESS".equals(status)) {
                colorHex = "#00C853";
                statusText = "Giao dịch thành công";
            } else if ("CANCELLED".equals(status)) {
                colorHex = "#e74c3c";
                statusText = "Đã từ chối / Hủy giao dịch";
            } else {
                colorHex = "#FF0000";
                statusText = "Trượt đấu giá";
            }

            VBox card = new VBox(15);
            card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 15; -fx-padding: 20;");

            HBox headerBox = new HBox(15);
            headerBox.setAlignment(Pos.CENTER_LEFT);

            // SỬA ĐOẠN NÀY: Cắt lấy 4 ký tự đầu của itemId (Giống hệt trang Đấu giá)
            String shortId = tx.itemId != null && tx.itemId.length() >= 4 ? tx.itemId.substring(0, 4).toUpperCase() : "N/A";
            Label lblId = new Label("SP" + shortId);
            lblId.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #2c3e50;");

            Label lblName = new Label(itemName);
            lblName.setStyle("-fx-font-size: 18px; -fx-text-fill: #333333;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label lblAmount = new Label(formatMoney(tx.amount));
            lblAmount.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 15px;");

            headerBox.getChildren().addAll(lblId, lblName, spacer, lblAmount);

            HBox detailBox = new HBox(20);
            detailBox.setAlignment(Pos.CENTER_LEFT);

            VBox imagePlaceholder = new VBox();
            imagePlaceholder.setPrefSize(100, 90);
            imagePlaceholder.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-border-color: #ddd; -fx-border-radius: 10;");

            VBox infoColumn = new VBox(8);
            infoColumn.setStyle("-fx-font-size: 15px;");

            infoColumn.getChildren().addAll(
                    createDetailRow("Ngày ghi nhận:", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), "#333333"),
                    createDetailRow("Địa chỉ giao hàng:", "Chưa cập nhật (Lấy từ Profile)", "#333333"),
                    createDetailRow("Trạng thái:", statusText, colorHex)
            );

            detailBox.getChildren().addAll(imagePlaceholder, infoColumn);
            card.getChildren().addAll(headerBox, detailBox);
            container.getChildren().add(card);
        }
    }

    private HBox createDetailRow(String title, String value, String valueColor) {
        HBox row = new HBox(10);
        Label lblTitle = new Label(title);
        lblTitle.setPrefWidth(200);
        lblTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333;");

        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-text-fill: " + valueColor + "; " + ("#333333".equals(valueColor) ? "" : "-fx-font-weight: bold;"));

        row.getChildren().addAll(lblTitle, lblValue);
        return row;
    }

    private String formatMoney(double amount) {
        return String.format("%,.0f", amount).replace(",", ".") + " VND";
    }

    @FXML
    public void handleBackToWallet(javafx.scene.input.MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/Wallet.fxml"));
            Node view = loader.load();
            Node source = (Node) event.getSource();
            Pane contentArea = (Pane) source.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
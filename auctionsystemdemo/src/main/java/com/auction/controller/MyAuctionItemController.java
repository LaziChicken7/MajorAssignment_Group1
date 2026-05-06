package com.auction.controller;

import com.auction.model.ApiResponse;
import com.auction.model.AuctionModel;
import com.auction.util.ApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class MyAuctionItemController {
    @FXML private Label lblId, lblName, lblPrice, lblStatus, lblTime;
    @FXML private Button btnStartAuction; // Nút mới

    private AuctionModel currentItem;

    public void setData(AuctionModel item) {
        if (item == null) return;
        this.currentItem = item;

        if (item.bidProduct != null) {
            String shortId = item.bidProduct.id != null && item.bidProduct.id.length() >= 4
                    ? item.bidProduct.id.substring(0, 4).toUpperCase() : "N/A";
            lblId.setText("SP: " + shortId);
            lblName.setText(item.bidProduct.name);
        }

        lblPrice.setText(String.format("%,.0f VND", item.highestBid).replace(",", "."));

        String time = item.endTime != null ? item.endTime.replace("T", " ") : "N/A";
        lblTime.setText(time);

        // Xử lý đổi màu chữ trạng thái
        String colorHex = "#e67e22";
        if ("FINISHED".equals(item.status) || "PAID".equals(item.status)) colorHex = "#2ecc71";
        else if ("CANCELLED".equals(item.status)) colorHex = "#e74c3c";
        else if ("OPEN".equals(item.status)) colorHex = "#3498db";

        lblStatus.setText(item.status);
        lblStatus.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-weight: bold; -fx-font-size: 16px;");

        // HIỂN THỊ NÚT "BẮT ĐẦU" NẾU ĐANG LÀ TRẠNG THÁI "OPEN"
        if ("OPEN".equals(item.status)) {
            btnStartAuction.setVisible(true);
            btnStartAuction.setManaged(true);
        } else {
            btnStartAuction.setVisible(false);
            btnStartAuction.setManaged(false);
        }
    }

    // XỬ LÝ KHI BẤM NÚT "BẮT ĐẦU PHIÊN"
    @FXML
    private void handleStartAuction() {
        if (currentItem == null) return;

        ApiService.putAsync("/auctions/" + currentItem.id + "/start", null).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() >= 200 && res.statusCode() < 300) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        // Thành công -> Đổi giao diện sang RUNNING ngay lập tức
                        currentItem.status = "RUNNING";
                        setData(currentItem);
                        showAlert("Thành công", "Đã mở phiên đấu giá cho sản phẩm " + currentItem.bidProduct.name);
                    }
                } else {
                    showAlert("Lỗi", "Không thể bắt đầu phiên đấu giá!");
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> showAlert("Lỗi kết nối", "Không thể kết nối đến máy chủ."));
            return null;
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content);
        alert.showAndWait();
    }
}
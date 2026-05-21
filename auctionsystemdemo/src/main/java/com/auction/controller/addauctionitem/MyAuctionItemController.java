package com.auction.controller.addauctionitem;

import com.auction.model.AuctionModel;
import com.auction.util.ApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public class MyAuctionItemController {

    @FXML private ImageView imgThumbnail;
    @FXML private Label lblId;
    @FXML private Label lblName;
    @FXML private Label lblPrice;
    @FXML private Label lblTime;
    @FXML private Label lblStatus;
    @FXML private Button btnStartAuction;

    private AuctionModel currentItem;
    private LocalDateTime parsedStartTime;
    private LocalDateTime parsedEndTime;

    public void setData(AuctionModel item) {
        this.currentItem = item;

        // 1. Ô TRỐNG THÌ IM LẶNG THOÁT RA (Không in dòng cảnh báo rác nữa để chống lag)
        if (item == null || item.bidProduct == null) return;

        String shortId = item.bidProduct.id != null && item.bidProduct.id.length() >= 4 ? item.bidProduct.id.substring(0, 4).toUpperCase() : "N/A";

        // Parse chuỗi thời gian
        try {
            if (item.startTime != null) parsedStartTime = LocalDateTime.parse(item.startTime.replace(" ", "T"));
            if (item.endTime != null) parsedEndTime = LocalDateTime.parse(item.endTime.replace(" ", "T"));
        } catch (DateTimeParseException ignored) {
            parsedStartTime = null; parsedEndTime = null;
        }

        // Dữ liệu cơ bản
        lblId.setText("SP" + shortId);
        lblName.setText(item.bidProduct.name);
        lblPrice.setText(String.format("%,.0f VND", item.highestBid).replace(",", "."));

        // Bo tròn ảnh
        imgThumbnail.setCache(true);
        imgThumbnail.setCacheHint(javafx.scene.CacheHint.SPEED);
        Rectangle clip = new Rectangle(120, 120);
        clip.setArcWidth(15);
        clip.setArcHeight(15);
        imgThumbnail.setClip(clip);

        // =========================================================
        // 2. GIỮ LẠI DÒNG IN LOG ĐỂ THEO DÕI LINK ẢNH
        // =========================================================
        if (item.bidProduct != null && item.bidProduct.imageUrls != null && !item.bidProduct.imageUrls.isEmpty()) {
            String imagePath = item.bidProduct.imageUrls.get(0);
            if (!imagePath.startsWith("/")) imagePath = "/" + imagePath;
            String fullUrl = ApiService.BASE_URL + imagePath + "?w=120&h=120";

            // In ra console để bạn dễ dàng debug
            System.out.println("🖼️ [UI] Yêu cầu tải ảnh SP" + shortId + ": " + fullUrl);

            com.auction.util.ImageCacheUtils.loadImage(
                    imgThumbnail, fullUrl, 120, 120, "https://via.placeholder.com/120?text=Loading..."
            );
        } else {
            System.out.println("⚠️ [UI] SP" + shortId + " không có ảnh, dùng ảnh mặc định.");

            com.auction.util.ImageCacheUtils.loadImage(
                    imgThumbnail, null, 120, 120, "https://via.placeholder.com/120?text=No+Image"
            );
        }

        // Logic Trạng thái
        String baseColor;
        switch (item.status) {
            case "OPEN":
                baseColor = "#f39c12";
                lblStatus.setText("SẮP MỞ");
                lblTime.setText(item.startTime != null ? item.startTime.replace("T", " ") : "--:--");
                btnStartAuction.setVisible(true);
                btnStartAuction.setManaged(true);
                break;
            case "RUNNING":
                baseColor = "#2ecc71";
                lblStatus.setText("ĐANG ĐẤU GIÁ");
                lblTime.setText(item.endTime != null ? item.endTime.replace("T", " ") : "--:--");
                break;
            case "FINISHED":
            case "PAID":
                baseColor = "#2ecc71";
                lblStatus.setText("ĐÃ KẾT THÚC");
                lblTime.setText(item.endTime != null ? item.endTime.replace("T", " ") : "--:--");
                break;
            case "CANCELLED":
                baseColor = "#e74c3c";
                lblStatus.setText("ĐÃ HỦY");
                lblTime.setText("--:--");
                break;
            default:
                baseColor = "#95a5a6";
                lblStatus.setText(item.status != null ? item.status : "UNKNOWN");
                lblTime.setText("--:--");
                break;
        }

        lblStatus.setStyle("-fx-text-fill: " + baseColor + "; -fx-font-weight: bold; -fx-font-size: 15px;");
        lblTime.setStyle("-fx-background-color: " + baseColor + "; -fx-text-fill: white; -fx-padding: 5 20; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 15px;");
    }

    @FXML
    private void handleStartAuction() {
        if (currentItem == null) return;

        ApiService.putAsync("/auctions/" + currentItem.id + "/start", null).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    try {
                        com.auction.model.ApiResponse apiRes = ApiService.gson.fromJson(res.body(), com.auction.model.ApiResponse.class);
                        if (apiRes.code == 1000) {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Thành công"); alert.setHeaderText(null);
                            alert.setContentText("Đã bắt đầu phiên đấu giá!");
                            alert.showAndWait();

                            btnStartAuction.setVisible(false); btnStartAuction.setManaged(false);
                            lblStatus.setText("ĐANG ĐẤU GIÁ");
                            lblStatus.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold; -fx-font-size: 15px;");
                            lblTime.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-padding: 5 20; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 15px;");
                        } else {
                            showErrorAlert(apiRes.message);
                        }
                    } catch (Exception e) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Thành công"); alert.setHeaderText(null);
                        alert.setContentText("Đã bắt đầu phiên đấu giá!");
                        alert.showAndWait();
                    }
                } else {
                    try {
                        com.auction.model.ApiResponse errRes = ApiService.gson.fromJson(res.body(), com.auction.model.ApiResponse.class);
                        showErrorAlert("Lỗi từ Server: " + errRes.message);
                    } catch (Exception e) {
                        showErrorAlert("Mã lỗi HTTP: " + res.statusCode());
                    }
                }
            });
        });
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        com.auction.util.AlertUtils.applyStyle(alert);
        alert.showAndWait();
    }
}
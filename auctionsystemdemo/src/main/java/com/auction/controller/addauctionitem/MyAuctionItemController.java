package com.auction.controller.addauctionitem;

import com.auction.model.AuctionModel;
import com.auction.util.ApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

public class MyAuctionItemController {

    @FXML private ImageView imgThumbnail;
    @FXML private Label lblId;
    @FXML private Label lblName;
    @FXML private Label lblPrice;
    @FXML private Label lblTime;
    @FXML private Label lblStatus;
    @FXML private Button btnStartAuction;

    private AuctionModel currentItem;

    public void setData(AuctionModel item) {
        // ==========================================
        // BƯỚC QUAN TRỌNG: XÓA DỮ LIỆU CŨ TRƯỚC KHI LOAD
        // (Chống lỗi rác giao diện khi cuộn ListView)
        // ==========================================
        imgThumbnail.setImage(null);
        lblId.setText("");
        lblName.setText("");
        lblPrice.setText("");
        lblTime.setText("");
        lblStatus.setText("");
        lblStatus.setStyle("");
        lblTime.setStyle("");
        btnStartAuction.setVisible(false);
        btnStartAuction.setManaged(false);
        // ==========================================

        this.currentItem = item;
        if (item == null || item.bidProduct == null) return;

        // 1. Dữ liệu cơ bản
        String shortId = item.bidProduct.id != null && item.bidProduct.id.length() >= 4 ? item.bidProduct.id.substring(0, 4).toUpperCase() : "N/A";
        lblId.setText("SP" + shortId);
        lblName.setText(item.bidProduct.name);
        lblPrice.setText(String.format("%,.0f VND", item.highestBid).replace(",", "."));

        // 2. Logic load ảnh
        imgThumbnail.setCache(true);
        imgThumbnail.setCacheHint(javafx.scene.CacheHint.SPEED);

        Rectangle clip = new Rectangle(120, 120);
        clip.setArcWidth(15);
        clip.setArcHeight(15);
        imgThumbnail.setClip(clip);

        if (item.bidProduct != null && item.bidProduct.imageUrls != null && !item.bidProduct.imageUrls.isEmpty()) {
            String imagePath = item.bidProduct.imageUrls.get(0);
            if (!imagePath.startsWith("/")) {
                imagePath = "/" + imagePath;
            }
            String fullUrl = ApiService.BASE_URL + imagePath + "?w=130&h=130";

            com.auction.util.ImageCacheUtils.loadImage(imgThumbnail, fullUrl, 130, 130, "https://via.placeholder.com/120?text=Loading...");
        } else {
            imgThumbnail.setImage(new Image("https://via.placeholder.com/120?text=No+Image", 130, 130, true, true, true));
        }

        // 3. Logic Thời gian, Trạng thái & Nút bắt đầu
        String baseColor;
        switch (item.status) {
            case "OPEN":
                baseColor = "#f39c12"; // Cam
                lblStatus.setText("SẮP MỞ");
                lblTime.setText(item.startTime != null ? item.startTime.replace("T", " ") : "--:--");
                btnStartAuction.setVisible(true);
                btnStartAuction.setManaged(true);
                break;
            case "RUNNING":
                baseColor = "#2ecc71"; // Xanh lá
                lblStatus.setText("ĐANG ĐẤU GIÁ");
                lblTime.setText(item.endTime != null ? item.endTime.replace("T", " ") : "--:--");
                break;
            case "FINISHED":
            case "PAID":
                baseColor = "#2ecc71"; // Xanh lá
                lblStatus.setText("ĐÃ KẾT THÚC");
                lblTime.setText(item.endTime != null ? item.endTime.replace("T", " ") : "--:--");
                break;
            case "CANCELLED":
                baseColor = "#e74c3c"; // Đỏ
                lblStatus.setText("ĐÃ HỦY");
                lblTime.setText("--:--");
                break;
            default:
                baseColor = "#95a5a6"; // Xám
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
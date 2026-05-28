package com.auction.controller.addauctionitem;

import com.auction.model.ApiResponse;
import com.auction.model.AuctionModel;
import com.auction.util.ApiService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.time.LocalDateTime;

@lombok.extern.slf4j.Slf4j
public class MyAuctionItemController {

    @FXML private Pane rootPane;
    @FXML private ImageView imgThumbnail;
    @FXML private Label lblId, lblName, lblPrice, lblTime, lblStatus;
    @FXML private Button btnStartAuction;

    private AuctionModel currentItem;
    private Timeline timeline;
    private LocalDateTime currentTargetTime;

    // HÀM AN TOÀN CHỐNG CRASH THỜI GIAN
    private LocalDateTime parseTimeSafely(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return null;
        try {
            timeStr = timeStr.contains("T") ? timeStr : timeStr.replace(" ", "T");
            if (timeStr.contains(".")) timeStr = timeStr.substring(0, timeStr.indexOf("."));
            if (timeStr.endsWith("Z")) timeStr = timeStr.replace("Z", "");
            if (timeStr.contains("+")) timeStr = timeStr.substring(0, timeStr.indexOf("+"));
            return LocalDateTime.parse(timeStr);
        } catch (Exception e) { return null; }
    }

    public void setData(AuctionModel item) {
        // 1. DỌN DẸP DỮ LIỆU CŨ CHỐNG KẸT RAM
        lblId.setText(""); lblName.setText(""); lblPrice.setText("");
        lblTime.setText(""); lblStatus.setText("");
        lblStatus.setStyle(""); lblTime.setStyle("");
        btnStartAuction.setVisible(false); btnStartAuction.setManaged(false);

        if (this.timeline != null) {
            this.timeline.stop();
            this.timeline = null;
        }

        this.currentItem = item;
        if (item == null || item.bidProduct == null) return;

        // 2. SET TEXT CƠ BẢN
        String shortId = item.bidProduct.id != null && item.bidProduct.id.length() >= 4 ? item.bidProduct.id.substring(0, 4).toUpperCase() : "N/A";
        lblId.setText("SP" + shortId);
        lblName.setText(item.bidProduct.name);
        lblPrice.setText(String.format("%,.0f VND", item.highestBid).replace(",", "."));

        // 3. ẢNH CACHE
        imgThumbnail.setCache(true); imgThumbnail.setCacheHint(javafx.scene.CacheHint.SPEED);
        Rectangle clip = new Rectangle(120, 120); clip.setArcWidth(15); clip.setArcHeight(15);
        imgThumbnail.setClip(clip);

        if (item.bidProduct.imageUrls != null && !item.bidProduct.imageUrls.isEmpty()) {
            String imagePath = item.bidProduct.imageUrls.get(0);
            if (!imagePath.startsWith("/")) imagePath = "/" + imagePath;
            String fullUrl = ApiService.BASE_URL + imagePath + "?w=120&h=120";
            com.auction.util.ImageCacheUtils.loadImage(imgThumbnail, fullUrl, 120, 120, "https://via.placeholder.com/120?text=Loading...");
        } else {
            com.auction.util.ImageCacheUtils.loadImage(imgThumbnail, null, 120, 120, "https://via.placeholder.com/120?text=No+Image");
        }

        // 4. MÀU SẮC TRẠNG THÁI
        String baseColor = "#95a5a6";
        switch (item.status) {
            case "OPEN": baseColor = "#f39c12"; lblStatus.setText("SẮP MỞ"); break;
            case "RUNNING": baseColor = "#2ecc71"; lblStatus.setText("ĐANG ĐẤU GIÁ"); break;
            case "FINISHED":
            case "PAID": baseColor = "#2ecc71"; lblStatus.setText("ĐÃ KẾT THÚC"); break;
            case "CANCELLED": baseColor = "#e74c3c"; lblStatus.setText("ĐÃ HỦY"); break;
            default: lblStatus.setText(item.status); break;
        }

        lblStatus.setStyle("-fx-text-fill: " + baseColor + "; -fx-font-weight: bold; -fx-font-size: 15px;");

        // Nếu Sắp mở -> Hiện nút Bắt đầu bằng tay cho Seller
        if ("OPEN".equals(item.status)) {
            btnStartAuction.setVisible(true);
            btnStartAuction.setManaged(true);
        }

        // 5. ĐỒNG HỒ ĐẾM NGƯỢC (TIMELINE)
        if (("RUNNING".equals(item.status) && item.endTime != null) || ("OPEN".equals(item.status) && item.startTime != null)) {
            try {
                String targetTimeStr = "OPEN".equals(item.status) ? item.startTime : item.endTime;
                currentTargetTime = parseTimeSafely(targetTimeStr);

                if (currentTargetTime == null) throw new Exception("Lỗi parse time");

                String finalBaseColor = baseColor;
                timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                    LocalDateTime now = LocalDateTime.now();
                    if (now.isAfter(currentTargetTime) || now.isEqual(currentTargetTime)) {
                        lblTime.setText("00:00:00");
                        lblTime.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-padding: 5 20; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 15px;");
                        btnStartAuction.setVisible(false); btnStartAuction.setManaged(false);
                        if (timeline != null) timeline.stop();
                    } else {
                        java.time.Duration duration = java.time.Duration.between(now, currentTargetTime);
                        long hours = duration.toHours();
                        long minutes = duration.toMinutesPart();
                        long seconds = duration.toSecondsPart();

                        lblTime.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                        if ("RUNNING".equals(item.status) && hours == 0 && minutes < 10) {
                            lblTime.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 5 20; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 15px;");
                        } else {
                            lblTime.setStyle("-fx-background-color: " + finalBaseColor + "; -fx-text-fill: white; -fx-padding: 5 20; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 15px;");
                        }
                    }
                }));
                timeline.setCycleCount(Timeline.INDEFINITE);
                timeline.play();
            } catch (Exception ex) {
                lblTime.setText("Lỗi định dạng");
            }
        } else {
            lblTime.setText("00:00:00");
            lblTime.setStyle("-fx-background-color: " + baseColor + "; -fx-text-fill: white; -fx-padding: 5 20; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 15px;");
        }
    }

    @FXML
    private void handleStartAuction() {
        log.info("\u25B6 Controller Action - Execute: handleStartAuction()");
        if (currentItem == null) return;

        ApiService.putAsync("/auctions/" + currentItem.id + "/start", null).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() >= 200 && res.statusCode() < 300) {
                    // Không cần làm gì cả, WebSocket sẽ tự động báo cho MyAuctionListController load lại danh sách!
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Đã bắt đầu phiên đấu giá!");
                    com.auction.util.AlertUtils.applyStyle(alert);
                    alert.showAndWait();
                } else {
                    try {
                        ApiResponse errRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        showErrorAlert("Lỗi từ Server: " + errRes.message);
                    } catch (Exception e) { showErrorAlert("Mã lỗi HTTP: " + res.statusCode()); }
                }
            });
        });
    }

    private void showErrorAlert(String message) {
        log.info("\u25B6 Controller Action - Execute: showErrorAlert()");
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        com.auction.util.AlertUtils.applyStyle(alert);
        alert.showAndWait();
    }
}
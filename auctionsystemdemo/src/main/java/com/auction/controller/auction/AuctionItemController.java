package com.auction.controller.auction;

import com.auction.model.AuctionModel;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import java.time.LocalDateTime;

public class AuctionItemController {

    // THÊM 2 BIẾN NÀY ĐỂ ĐIỀU KHIỂN NỀN VÀ HIỂN THỊ GIÁ CỦA TÔI
    @FXML private Pane rootPane;
    @FXML private Label lblMyBid;

    @FXML private ImageView imgThumbnail;
    @FXML private Label lblId, lblName, lblPrice, lblTime, lblTimeTitle;
    @FXML private Label lblStatus;

    private Timeline timeline;

    public void setData(AuctionModel item) {
        imgThumbnail.setFitWidth(120);
        imgThumbnail.setFitHeight(120);
        imgThumbnail.setPreserveRatio(true);

        // ==========================================
        // BƯỚC 1: XÓA DỮ LIỆU CŨ VÀ DỪNG TIMELINE
        // ==========================================
        lblId.setText("");
        lblName.setText("");
        lblPrice.setText("");
        lblTime.setText("");
        lblStatus.setText("");
        lblStatus.setStyle("");
        lblTime.setStyle("");
        if (lblTimeTitle != null) lblTimeTitle.setText("");

        // Reset lại hiển thị Giá Của Tôi
        if (lblMyBid != null) {
            lblMyBid.setText("");
            lblMyBid.setVisible(false);
            lblMyBid.setManaged(false);
        }

        // Xóa Class màu xanh đi (tránh bị kẹt màu cho item khác)
        if (rootPane != null) {
            rootPane.getStyleClass().remove("item-bidding-active");
        }

        if (this.timeline != null) {
            this.timeline.stop();
            this.timeline = null;
        }
        // ==========================================

        if (item == null) return;

        // BƯỚC 2: Set text cơ bản
        if (item.bidProduct != null) {
            String shortId = item.bidProduct.id != null && item.bidProduct.id.length() >= 4 ? item.bidProduct.id.substring(0, 4).toUpperCase() : "N/A";
            lblId.setText("SP" + shortId);
            lblName.setText(item.bidProduct.name);
        }

        lblPrice.setText(String.format("%,.0f VND", item.highestBid).replace(",", "."));

        // ==========================================
        // BƯỚC MỚI: ĐỌC "MỨC GIÁ TÔI ĐANG ĐẶT" TỪ SERVER GỬI VỀ
        // ==========================================
        double myHighestBid = item.myHighestBid; // Đọc thẳng luôn, không cần vòng lặp nữa!

        // NẾU TÔI CÓ THAM GIA ĐẤU GIÁ SẢN PHẨM NÀY
        if (myHighestBid > 0) {
            // Hiển thị Label
            if (lblMyBid != null) {
                lblMyBid.setText("Bạn đã đặt: " + String.format("%,.0f VND", myHighestBid).replace(",", "."));
                lblMyBid.setVisible(true);
                lblMyBid.setManaged(true);
            }
            // Bật nền màu xanh
            if (rootPane != null) {
                rootPane.getStyleClass().add("item-bidding-active");
            }
        }
        // ==========================================

        // BƯỚC 3: XỬ LÝ ẢNH BẰNG CACHE
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
            imgThumbnail.setImage(new javafx.scene.image.Image("https://via.placeholder.com/120?text=No+Image", 130, 130, true, true, true));
        }

        // BƯỚC 4: Trạng thái và Màu sắc
        String baseColor;
        switch (item.status) {
            case "RUNNING": baseColor = "#f39c12"; break;
            case "OPEN": baseColor = "#3498db"; break;
            case "FINISHED":
            case "PAID": baseColor = "#2ecc71"; break;
            case "CANCELLED": baseColor = "#e74c3c"; break;
            default: baseColor = "#95a5a6"; break;
        }

        lblStatus.setText(item.status != null ? item.status : "UNKNOWN");
        lblStatus.setStyle("-fx-text-fill: " + baseColor + "; -fx-font-weight: bold; -fx-font-size: 15px;");

        if (lblTimeTitle != null) {
            if ("OPEN".equals(item.status)) lblTimeTitle.setText("Sắp bắt đầu sau:");
            else if ("RUNNING".equals(item.status)) lblTimeTitle.setText("Thời gian còn lại:");
            else lblTimeTitle.setText("Thời gian:");
        }

        // BƯỚC 5: Tạo Timeline đếm ngược
        if (("RUNNING".equals(item.status) && item.endTime != null) || ("OPEN".equals(item.status) && item.startTime != null)) {
            try {
                String targetTimeStr = "OPEN".equals(item.status) ? item.startTime : item.endTime;
                targetTimeStr = targetTimeStr.contains("T") ? targetTimeStr : targetTimeStr.replace(" ", "T");
                LocalDateTime targetTime = LocalDateTime.parse(targetTimeStr);

                timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                    LocalDateTime now = LocalDateTime.now();
                    if (now.isAfter(targetTime)) {
                        lblTime.setText("00:00:00");
                        lblTime.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-padding: 5 20; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 15px;");
                        if (timeline != null) timeline.stop();
                    } else {
                        java.time.Duration duration = java.time.Duration.between(now, targetTime);
                        long hours = duration.toHours();
                        long minutes = duration.toMinutesPart();
                        long seconds = duration.toSecondsPart();

                        lblTime.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                        if ("RUNNING".equals(item.status) && hours == 0 && minutes < 10) {
                            lblTime.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 5 20; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 15px;");
                        } else {
                            lblTime.setStyle("-fx-background-color: " + baseColor + "; -fx-text-fill: white; -fx-padding: 5 20; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 15px;");
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
}
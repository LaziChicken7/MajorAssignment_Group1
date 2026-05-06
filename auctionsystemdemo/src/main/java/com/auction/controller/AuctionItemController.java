package com.auction.controller;

import com.auction.model.AuctionModel;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.time.LocalDateTime;

public class AuctionItemController {

    @FXML private Label lblId, lblName, lblPrice, lblTime;
    @FXML private Label lblStatus; // Nhãn trạng thái mới thêm

    private Timeline timeline;

    public void setData(AuctionModel item) {
        if (item == null) return;

        // 1. Set ID và Tên
        if (item.bidProduct != null) {
            String shortId = item.bidProduct.id != null && item.bidProduct.id.length() >= 4
                    ? item.bidProduct.id.substring(0, 4).toUpperCase() : "N/A";
            lblId.setText("SP: " + shortId);
            lblName.setText(item.bidProduct.name);
        }

        // 2. Set Giá
        lblPrice.setText(String.format("%,.0f VND", item.highestBid).replace(",", "."));

        // 3. SET TRẠNG THÁI VÀ MÀU SẮC (Đồng bộ)
        String baseColor;
        switch (item.status) {
            case "RUNNING": baseColor = "#f39c12"; break; // Vàng cam
            case "OPEN": baseColor = "#3498db"; break; // Xanh dương sáng
            case "FINISHED":
            case "PAID": baseColor = "#2ecc71"; break; // Xanh lá
            case "CANCELLED": baseColor = "#e74c3c"; break; // Đỏ
            default: baseColor = "#95a5a6"; break; // Xám
        }

        lblStatus.setText(item.status);
        lblStatus.setStyle("-fx-text-fill: " + baseColor + "; -fx-font-weight: bold; -fx-font-size: 16px;");

        // 4. XỬ LÝ ĐỒNG HỒ ĐẾM NGƯỢC
        if (timeline != null) timeline.stop();

        if (item.endTime != null && ("RUNNING".equals(item.status) || "OPEN".equals(item.status))) {
            try {
                String timeStr = item.endTime.contains("T") ? item.endTime : item.endTime.replace(" ", "T");
                LocalDateTime endTime = LocalDateTime.parse(timeStr);

                timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                    LocalDateTime now = LocalDateTime.now();

                    if (now.isAfter(endTime)) {
                        lblTime.setText("00:00:00");
                        lblTime.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-padding: 6 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 16px;");
                        timeline.stop();
                    } else {
                        java.time.Duration duration = java.time.Duration.between(now, endTime);
                        long hours = duration.toHours();
                        long minutes = duration.toMinutesPart();
                        long seconds = duration.toSecondsPart();

                        lblTime.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));

                        // Cảnh báo khẩn cấp: Dưới 10 phút thì ô thời gian nháy đỏ (dù trạng thái là gì)
                        if ("RUNNING".equals(item.status) && hours == 0 && minutes < 10) {
                            lblTime.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 6 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 16px;");
                        } else {
                            // Ô thời gian đổi nền theo màu của trạng thái
                            lblTime.setStyle("-fx-background-color: " + baseColor + "; -fx-text-fill: white; -fx-padding: 6 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 16px;");
                        }
                    }
                }));
                timeline.setCycleCount(Timeline.INDEFINITE);
                timeline.play();

            } catch (Exception ex) {
                lblTime.setText("Lỗi định dạng");
            }
        } else {
            // Đối với các trạng thái FINISHED, PAID, CANCELLED
            lblTime.setText("00:00:00");
            lblTime.setStyle("-fx-background-color: " + baseColor + "; -fx-text-fill: white; -fx-padding: 6 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 16px;");
        }
    }
}
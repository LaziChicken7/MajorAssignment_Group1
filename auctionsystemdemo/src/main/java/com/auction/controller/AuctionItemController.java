package com.auction.controller;

import com.auction.model.AuctionModel;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.util.Duration;
import java.time.LocalDateTime;

public class AuctionItemController {

    @FXML private Label lblId, lblName, lblPrice, lblTime, lblTimeTitle;
    @FXML private Label lblStatus;

    private Timeline timeline;

    public void setData(AuctionModel item) {
        if (item == null) return;

        if (item.bidProduct != null) {
            String shortId = item.bidProduct.id != null && item.bidProduct.id.length() >= 4 ? item.bidProduct.id.substring(0, 4).toUpperCase() : "N/A";
            lblId.setText("SP" + shortId);
            lblName.setText(item.bidProduct.name);
        }

        lblPrice.setText(String.format("%,.0f VND", item.highestBid).replace(",", "."));

        String baseColor;
        switch (item.status) {
            case "RUNNING": baseColor = "#f39c12"; break;
            case "OPEN": baseColor = "#3498db"; break;
            case "FINISHED":
            case "PAID": baseColor = "#2ecc71"; break;
            case "CANCELLED": baseColor = "#e74c3c"; break;
            default: baseColor = "#95a5a6"; break;
        }

        lblStatus.setText(item.status);
        lblStatus.setStyle("-fx-text-fill: " + baseColor + "; -fx-font-weight: bold; -fx-font-size: 16px;");

        // ĐỔI CHỮ LINH ĐỘNG THEO TRẠNG THÁI
        if (lblTimeTitle != null) {
            if ("OPEN".equals(item.status)) lblTimeTitle.setText("Sắp bắt đầu sau:");
            else if ("RUNNING".equals(item.status)) lblTimeTitle.setText("Thời gian còn lại:");
            else lblTimeTitle.setText("Thời gian:");
        }

        if (timeline != null) timeline.stop();

        if (("RUNNING".equals(item.status) && item.endTime != null) || ("OPEN".equals(item.status) && item.startTime != null)) {
            try {
                // Nếu OPEN -> Đếm ngược đến startTime. Nếu RUNNING -> Đếm ngược đến endTime.
                String targetTimeStr = "OPEN".equals(item.status) ? item.startTime : item.endTime;
                targetTimeStr = targetTimeStr.contains("T") ? targetTimeStr : targetTimeStr.replace(" ", "T");
                LocalDateTime targetTime = LocalDateTime.parse(targetTimeStr);

                timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                    LocalDateTime now = LocalDateTime.now();

                    if (now.isAfter(targetTime)) {
                        lblTime.setText("00:00:00");
                        lblTime.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-padding: 6 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 16px;");
                        timeline.stop();
                    } else {
                        java.time.Duration duration = java.time.Duration.between(now, targetTime);
                        long hours = duration.toHours();
                        long minutes = duration.toMinutesPart();
                        long seconds = duration.toSecondsPart();

                        lblTime.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));

                        if ("RUNNING".equals(item.status) && hours == 0 && minutes < 10) {
                            lblTime.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 6 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 16px;");
                        } else {
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
            lblTime.setText("00:00:00");
            lblTime.setStyle("-fx-background-color: " + baseColor + "; -fx-text-fill: white; -fx-padding: 6 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 16px;");
        }
    }
}
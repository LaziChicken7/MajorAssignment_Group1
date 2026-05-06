package com.auction.controller;

import com.auction.model.AuctionModel;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MyAuctionItemController {
    @FXML private Label lblId, lblName, lblPrice, lblStatus, lblTime;

    public void setData(AuctionModel item) {
        if (item == null) return;

        // Xử lý ID và Tên
        if (item.bidProduct != null) {
            String shortId = item.bidProduct.id != null && item.bidProduct.id.length() >= 4
                    ? item.bidProduct.id.substring(0, 4).toUpperCase()
                    : "N/A";
            lblId.setText("SP: " + shortId);
            lblName.setText(item.bidProduct.name);
        }

        // Xử lý giá tiền
        lblPrice.setText(String.format("%,.0f VND", item.highestBid).replace(",", "."));
        lblStatus.setText(item.status);

        // Đổi màu trạng thái chữ
        String colorHex = "#e67e22";
        if ("FINISHED".equals(item.status) || "PAID".equals(item.status)) {
            colorHex = "#2ecc71"; // Xanh
        } else if ("CANCELLED".equals(item.status)) {
            colorHex = "#e74c3c"; // Đỏ
        }
        lblStatus.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-weight: bold;");

        // Thời gian
        String time = item.endTime != null ? item.endTime.replace("T", " ") : "N/A";
        lblTime.setText(time);

        // Đổi màu nền ô thời gian
        if ("RUNNING".equalsIgnoreCase(item.status) || "OPEN".equalsIgnoreCase(item.status)) {
            lblTime.setStyle("-fx-background-color: #ffbd59; -fx-text-fill: black; -fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold;");
        } else {
            lblTime.setStyle("-fx-background-color: #ff3b30; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold;");
        }
    }
}
package com.auction.controller;

import com.auction.model.AuctionItem;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MyAuctionItemController {
    @FXML private Label lblId, lblName, lblPrice, lblStatus, lblTime;

    public void setData(AuctionItem item) {
        if (item == null) return;

        lblId.setText(item.getId());
        lblName.setText(item.getName());
        lblPrice.setText(String.format("%,.0f VND", item.getCurrentPrice()));
        lblStatus.setText(item.getStatus());

        // Đổi màu chữ trạng thái (Xanh cho SUCCESS, Đỏ cho CLOSED, Cam cho RUNNING)
        lblStatus.setStyle("-fx-text-fill: " + item.getStatusColor() + "; -fx-font-weight: bold;");

        lblTime.setText(item.getTimeLeft());

        // Đổi màu nền ô thời gian
        if ("RUNNING".equalsIgnoreCase(item.getStatus())) {
            lblTime.setStyle("-fx-background-color: #ffbd59; -fx-text-fill: black; -fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold;");
        } else {
            lblTime.setStyle("-fx-background-color: #ff3b30; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold;");
        }
    }
}
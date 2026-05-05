package com.auction.controller;

import com.auction.model.AuctionItem;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AuctionItemController {
    @FXML private Label lblId;
    @FXML private Label lblName;
    @FXML private Label lblPrice;
    @FXML private Label lblTime;
    @FXML private Label lblStatus; // Khai báo thêm nhãn trạng thái

    public void setData(AuctionItem item) {
        if (item == null) return;

        lblId.setText(item.getId());
        lblName.setText(item.getName());

        // ĐỊNH DẠNG TIỀN: Quan trọng để giao diện gọn gàng
        // %,.0f sẽ biến 1000000 thành 1.000.000
        lblPrice.setText(String.format("%,.0f VND", item.getCurrentPrice()));

        lblTime.setText(item.getTimeLeft());

        // Hiển thị trạng thái và đổi màu chữ
        if (lblStatus != null) {
            lblStatus.setText(item.getStatus());
            lblStatus.setStyle("-fx-text-fill: " + item.getStatusColor() + ";");
        }
    }
}
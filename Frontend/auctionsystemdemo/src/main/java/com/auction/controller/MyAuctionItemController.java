package com.auction.controller;

import com.auction.model.AuctionItem;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MyAuctionItemController {

    // BẠN ĐANG THIẾU PHẦN KHAI BÁO NÀY NÊN NÓ BÁO ĐỎ LBLID, LBLNAME...
    @FXML private Label lblId;
    @FXML private Label lblName;
    @FXML private Label lblPrice;
    @FXML private Label lblStatus;
    @FXML private Label lblTime;

    public void setData(AuctionItem a) {
        // Set ID và tên
        lblId.setText(a.getId());
        lblName.setText(a.getName());

        // Định dạng giá tiền có dấu chấm phân cách (Vd: 1.273.000 VND)
        lblPrice.setText(String.format("%,.0f VND", a.getCurrentPrice()));

        // Set trạng thái (SUCCESS, RUNNING, CLOSED)
        lblStatus.setText(a.getStatus());

        // Đổi màu chữ trạng thái dựa theo màu lấy từ Model
        lblStatus.setStyle("-fx-text-fill: " + a.getStatusColor() + "; -fx-font-weight: bold;");

        // Set thời gian còn lại
        lblTime.setText(a.getTimeLeft());

        // Đổi màu nền cho ô Thời gian dựa trên trạng thái (như ảnh mẫu)
        if ("RUNNING".equalsIgnoreCase(a.getStatus())) {
            lblTime.setStyle("-fx-background-color: #ffbd59; -fx-text-fill: black; -fx-padding: 5 20; -fx-background-radius: 20; -fx-font-weight: bold;");
        } else {
            lblTime.setStyle("-fx-background-color: #ff3b30; -fx-text-fill: white; -fx-padding: 5 20; -fx-background-radius: 20; -fx-font-weight: bold;");
        }
    }
}
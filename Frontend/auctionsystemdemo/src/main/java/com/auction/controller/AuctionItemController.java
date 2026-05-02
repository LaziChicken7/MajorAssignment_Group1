package com.auction.controller;

import com.auction.model.AuctionItem;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

// DÒNG QUAN TRỌNG NHẤT: Phải khai báo Class bao quanh các hàm
public class AuctionItemController {

    // Khai báo các Label phải khớp với fx:id trong file FXML
    @FXML private Label lblId;
    @FXML private Label lblName;
    @FXML private Label lblPrice;
    @FXML private Label lblTime;

    public void setData(AuctionItem a) {
        lblId.setText(a.getId());
        lblName.setText(a.getName());
        lblPrice.setText(String.format("%,.0f VND", a.getCurrentPrice()));
        lblTime.setText(a.getTimeLeft());
    }
} // Nhớ đóng ngoặc nhọn kết thúc Class
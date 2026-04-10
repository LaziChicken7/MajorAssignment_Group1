package com.auction.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class AuctionController {

    @FXML
    public void handlePlaceBid(ActionEvent event) {
        // Logic khi ấn nút Đấu giá ở Page 12/13
        System.out.println("Hiện popup Xác nhận đấu giá (Page 13)...");
    }

    @FXML
    public void handleConfirmBid(ActionEvent event) {
        // Logic khi ấn nút "Xác nhận" trong popup (Page 14)
        System.out.println("Đấu giá thành công hoặc thất bại!");
    }
}
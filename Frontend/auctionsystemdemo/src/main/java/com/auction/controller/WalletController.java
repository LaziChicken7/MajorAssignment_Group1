package com.auction.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class WalletController {

    @FXML
    public void handleDeposit(ActionEvent event) {
        // Hiển thị popup nạp tiền (Page 6)
        System.out.println("Mở popup Nạp tiền...");
    }

    @FXML
    public void handleWithdraw(ActionEvent event) {
        // Thực hiện rút tiền (Page 8)
        System.out.println("Thực hiện Rút tiền...");
    }
}
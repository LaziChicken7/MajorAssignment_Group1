package com.auction.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class LoginController {

    @FXML
    public void handleLoginClick(ActionEvent event) {
        System.out.println("Người dùng bấm nút Đăng nhập");
        // Chèn logic mở cửa sổ Đăng nhập hoặc gọi API tại đây
    }

    @FXML
    public void handleRegisterClick(ActionEvent event) {
        System.out.println("Người dùng bấm nút Đăng ký");
    }
}
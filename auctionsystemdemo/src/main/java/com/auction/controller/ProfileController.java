package com.auction.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class ProfileController {

    @FXML
    public void handleUpdateProfile(ActionEvent event) {
        System.out.println("Đang lưu thông tin cá nhân mới...");
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        System.out.println("Đăng xuất, chuyển về màn hình Login!");
        // Gọi hàm đổi scene để quay về Login.fxml
    }
}
package com.auction.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class HomeController {

    @FXML
    public void initialize() {
        // Hàm này chạy ngay khi Dashboard vừa load xong
        // Bạn có thể fetch dữ liệu biểu đồ hoặc danh sách SP nổi bật ở đây
        System.out.println("Load dữ liệu trang Dashboard...");
    }

    @FXML
    public void handleViewMoreAuctions(ActionEvent event) {
        System.out.println("Xem thêm sản phẩm nổi bật...");
    }

    @FXML
    public void handleViewMoreNotifications(ActionEvent event) {
        System.out.println("Xem thêm thông báo...");
    }
}
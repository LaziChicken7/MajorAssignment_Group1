package com.auction.controller.addauctionitem;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class ProductController {

    @FXML
    public void handleUploadImage(ActionEvent event) {
        System.out.println("Mở FileChooser để chọn ảnh sản phẩm...");
    }

    @FXML
    public void handleSubmitProduct(ActionEvent event) {
        // Lấy dữ liệu từ các TextField và DatePicker để lưu vào DB
        System.out.println("Xác nhận thêm sản phẩm mới (Page 18)...");
    }
}
package com.auction.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Stage;

public class DepositController {
    @FXML
    private TextField amountField;

    @FXML
    void handleConfirm(ActionEvent event) {
        System.out.println("Tiền nạp: " + amountField.getText());
    }

    @FXML
    void handleCancel(ActionEvent event) {
        // Đóng cửa sổ nếu là popup
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    void handleBack(ActionEvent event) {
        // Xử lý quay lại trang trước
        System.out.println("Quay lại màn hình ví...");
    }
}
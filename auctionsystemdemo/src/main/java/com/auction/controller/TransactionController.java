package com.auction.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent; // Dùng MouseEvent thay vì ActionEvent
import javafx.scene.layout.Pane;
import java.io.IOException;

public class TransactionController {

    @FXML
    public void handleBackToWallet(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/Wallet.fxml"));
            Node walletView = loader.load();

            Node source = (Node) event.getSource();

            Pane mainDisplay = (Pane) source.getScene().lookup("#contentArea");

            if (mainDisplay != null) {
                mainDisplay.getChildren().setAll(walletView);
            } else {
                System.out.println("Lỗi: Không tìm thấy vùng chứa gốc (mainDisplay)!");
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Lỗi: Không thể tải file Wallet.fxml. Hãy kiểm tra lại đường dẫn.");
        }
    }
}
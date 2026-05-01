package com.auction.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import java.io.IOException;

public class TransactionController {

    @FXML
    private Button btnBack;

    @FXML
    public void initialize() {
        if (btnBack != null) {
            btnBack.setOnAction(this::handleBackToWallet);
        }
    }

    @FXML
    private void handleBackToWallet(ActionEvent event) {
        try {
            // Nạp lại file Wallet.fxml vào contentArea
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/Wallet.fxml"));
            Node walletView = loader.load();

            // Tìm contentArea dựa vào id
            VBox contentArea = (VBox) btnBack.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(walletView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
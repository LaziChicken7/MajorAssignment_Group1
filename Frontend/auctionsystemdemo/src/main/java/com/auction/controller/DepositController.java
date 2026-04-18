package com.auction.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;

public class DepositController {
    @FXML
    private TextField amountField;

    @FXML
    void handleConfirm(ActionEvent event) {
        try {
            Node successView = FXMLLoader.load(getClass().getResource("/com/auction/view/DepositSuccess.fxml"));

            Node source = (Node) event.getSource();

            Pane contentArea = (Pane) source.getScene().lookup("#contentArea");

            if (contentArea != null) {
                contentArea.getChildren().setAll(successView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    void handleBackToWallet(ActionEvent event) {
        try {
            Node walletView = FXMLLoader.load(getClass().getResource("/com/auction/view/Wallet.fxml"));

            VBox contentArea = (VBox) ((Node) event.getSource()).getScene().lookup("#contentArea");

            if (contentArea != null) {
                // Quay lại giao diện ví ban đầu (hiện lại lịch sử giao dịch)
                contentArea.getScene().setRoot((Parent) walletView);
            } else {
                // Chữa cháy nếu không tìm thấy vùng chứa:
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.getScene().setRoot((Parent) walletView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        // Nếu đang hiện trong contentArea, chỉ cần load lại Wallet
        handleBackToWallet(event);
    }

    @FXML
    void handleBack(ActionEvent event) {
        handleBackToWallet(event);
    }
}
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/DepositSuccess.fxml"));
            Node successView = loader.load();

            Scene scene = ((Node) event.getSource()).getScene();
            Pane contentArea = (Pane) scene.lookup("#contentArea");

            if (contentArea != null) {
                // Thay thế nội dung cũ (Form nạp tiền) bằng nội dung mới (Thông báo thành công)
                contentArea.getChildren().setAll(successView);
            } else {
                System.err.println("Không tìm thấy vùng chứa #contentArea!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    void handleBackToWallet(ActionEvent event) {
        try {
            Node walletView = FXMLLoader.load(getClass().getResource("/com/auction/view/Wallet.fxml"));
            Scene scene = ((Node) event.getSource()).getScene();
            Pane contentArea = (Pane) scene.lookup("#contentArea");

            if (contentArea != null) {
                contentArea.getChildren().setAll(walletView);
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
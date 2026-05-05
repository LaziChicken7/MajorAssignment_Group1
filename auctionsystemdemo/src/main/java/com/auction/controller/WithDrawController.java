package com.auction.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.event.ActionEvent;
import java.io.IOException;

public class WithDrawController {

    @FXML
    private TextField amountField;

    @FXML
    void handleWithdrawConfirm(ActionEvent event) {
        try {
            // Kiểm tra xem đã nhập tiền chưa
            System.out.println("Đang xử lý rút tiền: " + amountField.getText());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/WithDrawSuccess.fxml"));

            //Rút tiền thất bại
            //FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/WithDrawFail.fxml"));

            Node successView = loader.load();

            Scene scene = ((Node) event.getSource()).getScene();
            Pane contentArea = (Pane) scene.lookup("#contentArea");

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
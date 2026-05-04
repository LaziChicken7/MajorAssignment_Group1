package com.auction.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class WalletController {

    @FXML
    private VBox contentArea;

    @FXML
    public void handleDeposit(ActionEvent event) {
        // Hiển thị popup nạp tiền (Page 6)
        try {
            // 1. Tải cái form nạp tiền nhỏ
            Node depositForm = FXMLLoader.load(getClass().getResource("/com/auction/view/DepositForm.fxml"));

            contentArea.getChildren().setAll(depositForm);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Mở popup lên Nạp tiền...");
        }
    }

    @FXML
    public void handleWithdraw(ActionEvent event) {
        // Thực hiện rút tiền (Page 8)
        try {
            Node withdrawView = FXMLLoader.load(getClass().getResource("/com/auction/view/WithdrawForm.fxml"));
            // Tìm contentArea và setAll(withdrawView)
            contentArea.getChildren().setAll(withdrawView);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Thực hiện Rút tiền...");
        }
    }

    @FXML
    public void handleSuccessfulTransaction(ActionEvent event) {
        try {
            // Nạp giao diện SuccessfulTransaction.fxml vào contentArea thay vì mở stage mới
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/SuccessfulTransaction.fxml"));
            Node successView = loader.load();

            contentArea.getChildren().setAll(successView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleFailureTransaction(ActionEvent event) {
        try {
            // Nạp giao diện SuccessfulTransaction.fxml vào contentArea thay vì mở stage mới
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/FailureTransaction.fxml"));
            Node successView = loader.load();

            contentArea.getChildren().setAll(successView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
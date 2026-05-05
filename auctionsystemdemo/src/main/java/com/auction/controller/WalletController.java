package com.auction.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class WalletController {

    @FXML
    private VBox historySection;
    @FXML
    private VBox walletRoot;

    @FXML
    public void handleDeposit(ActionEvent event) {
        // Hiển thị popup nạp tiền (Page 6)
        try {
            // 1. Tải cái form nạp tiền nhỏ
            Node depositForm = FXMLLoader.load(getClass().getResource("/com/auction/view/DepositForm.fxml"));

            historySection.getChildren().setAll(depositForm);

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
            historySection.getChildren().setAll(withdrawView);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Thực hiện Rút tiền...");
        }
    }

    @FXML
    public void handleSuccessfulTransaction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/SuccessfulTransaction.fxml"));
            Node successView = loader.load();

            walletRoot.getChildren().setAll(successView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleFailureTransaction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/FailureTransaction.fxml"));
            Node successView = loader.load();

            walletRoot.getChildren().setAll(successView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Ẩn số dư
    @FXML
    private TextField balanceField;

    @FXML
    private Label eyeIconText;

    private boolean isBalanceVisible = false;
    private final String ACTUAL_BALANCE = "17.125.000";

    @FXML
    public void toggleBalanceVisibility(MouseEvent event) {
        isBalanceVisible = !isBalanceVisible;

        if (isBalanceVisible) {
            // Hiện số dư
            balanceField.setText(ACTUAL_BALANCE + " VND");
            eyeIconText.setText("Ẩn");
            eyeIconText.setStyle("-fx-font-size: 16px; -fx-text-fill: #FF0000; -fx-font-weight: bold; -fx-cursor: hand;"); // Tùy chọn: Đổi màu chữ sang đỏ cho khác biệt
        } else {
            // Ẩn số dư
            balanceField.setText("****** VND");
            eyeIconText.setText("Hiện");
            eyeIconText.setStyle("-fx-font-size: 16px; -fx-text-fill: #0A439D; -fx-font-weight: bold; -fx-cursor: hand;"); // Đổi lại màu xanh
        }
    }
}
package com.auction.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;

public class NotificationController {

    @FXML
    private Button backButton;

    @FXML
    private VBox notificationContainer;

    @FXML
    void handleBackButton(ActionEvent event) {
        System.out.println("Back button clicked");
    }

    @FXML
    void handleAcceptNotification(ActionEvent event) {
        System.out.println("Notification accepted");
    }

    @FXML
    void handleRejectNotification(ActionEvent event) {
        System.out.println("Notification rejected");
        // TODO: Process rejection
    }

    @FXML
    void handleDeleteNotification(ActionEvent event) {
        // Handle delete notification action
        System.out.println("Notification deleted");
        // TODO: Delete notification from list
    }

    @FXML
    void handleMoreOptions(ActionEvent event) {
        // Handle more options action
        System.out.println("More options clicked");
        // TODO: Show context menu or more options
    }
}

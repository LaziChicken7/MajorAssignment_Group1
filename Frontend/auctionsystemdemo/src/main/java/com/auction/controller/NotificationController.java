package com.auction.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

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

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/ShowNotification.fxml"));
            Parent root = loader.load();

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initStyle(StageStyle.TRANSPARENT);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            popupStage.setScene(scene);


            Stage parentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene parentScene = ((Node) event.getSource()).getScene();

            popupStage.initOwner(parentStage);

            popupStage.setX(parentStage.getX() + parentScene.getX());
            popupStage.setY(parentStage.getY() + parentScene.getY());
            popupStage.setWidth(parentScene.getWidth());
            popupStage.setHeight(parentScene.getHeight());

            popupStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleClose(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
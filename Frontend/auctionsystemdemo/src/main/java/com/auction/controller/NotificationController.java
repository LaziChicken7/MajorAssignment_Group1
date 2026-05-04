package com.auction.controller;

import com.auction.model.Notification;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import java.io.IOException;

public class NotificationController {
    @FXML private ListView<Notification> lvNotifications;
    @FXML private VBox panePopupDetail; // Khai báo cái lớp mờ popup
    @FXML private Label lblPopupContent; // Khai báo nhãn nội dung trong popup

    @FXML
    public void initialize() {
        // KẾT NỐI VỚI DỮ LIỆU THẬT
        lvNotifications.setItems(NotificationService.getNotifications());

        lvNotifications.setCellFactory(param -> new ListCell<Notification>() {
            @Override
            protected void updateItem(Notification item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/NotificationItem.fxml"));
                        setGraphic(loader.load());
                        ((NotificationItemController)loader.getController()).setData(item);
                    } catch (IOException e) { e.printStackTrace(); }
                }
            }
        });
    }

    @FXML
    public void closePopup() {
        if (panePopupDetail != null) {
            panePopupDetail.setVisible(false); // Ẩn popup đi
        }
    }
}
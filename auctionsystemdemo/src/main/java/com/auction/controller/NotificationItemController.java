package com.auction.controller;

import com.auction.model.Notification;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class NotificationItemController {
    @FXML private Label lblMessage;
    @FXML private Button btnAccept, btnReject, btnDetail;

    public void setData(Notification notif) {
        lblMessage.setText(notif.getMessage());

        // Nếu là loại thành công thì đổi icon hoặc ẩn nút tùy ý
        if ("SUCCESS".equals(notif.getType())) {
            btnAccept.setVisible(false);
            btnAccept.setManaged(false);
            btnReject.setText("☒"); // Icon giống trong ảnh của bạn
        }
    }

    public Button getBtnDetail() { return btnDetail; }
}
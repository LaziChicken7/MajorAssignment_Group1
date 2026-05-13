package com.auction.controller.notification;

import com.auction.model.NotificationModel;
import com.auction.util.ApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class NotificationItemController {

    @FXML private Label lblTitle, lblDescription, lblTime;
    @FXML private Button btnAccept, btnDecline, btnDelete;

    private NotificationModel currentItem;
    private Runnable refreshCallback;

    public void setData(NotificationModel item, Runnable refreshCallback) {
        this.currentItem = item;
        this.refreshCallback = refreshCallback;

        lblTitle.setText(item.title);
        lblDescription.setText(item.description);

        String time = item.createdAt != null ? item.createdAt.replace("T", " ") : "";
        lblTime.setText(time);

        // HIỂN THỊ NÚT BẤM DỰA THEO LOẠI THÔNG BÁO
        if ("PAYMENT_VERIFICATION".equals(item.type)) {
            // Yêu cầu xác thực -> Bật Xanh/Đỏ, Tắt Xóa
            btnAccept.setVisible(true); btnAccept.setManaged(true);
            btnDecline.setVisible(true); btnDecline.setManaged(true);
            btnDelete.setVisible(false); btnDelete.setManaged(false);
        } else {
            // Thành công/Thất bại -> Chỉ bật Xóa
            btnAccept.setVisible(false); btnAccept.setManaged(false);
            btnDecline.setVisible(false); btnDecline.setManaged(false);
            btnDelete.setVisible(true); btnDelete.setManaged(true);
        }
    }

    @FXML
    private void handleAccept() {
        ApiService.putAsync("/notifications/" + currentItem.notificationId + "/accept", null)
                .thenAccept(res -> handleResponse(res.statusCode(), "Xác nhận thanh toán thành công!"));
    }

    @FXML
    private void handleDecline() {
        ApiService.putAsync("/notifications/" + currentItem.notificationId + "/decline", null)
                .thenAccept(res -> handleResponse(res.statusCode(), "Đã từ chối thanh toán!"));
    }

    @FXML
    private void handleDelete() {
        ApiService.deleteAsync("/notifications/" + currentItem.notificationId)
                .thenAccept(res -> handleResponse(res.statusCode(), null)); // Null msg để không nhảy Popup thông báo
    }

    // Hàm xử lý chung sau khi gọi API
    private void handleResponse(int statusCode, String successMsg) {
        Platform.runLater(() -> {
            if (statusCode >= 200 && statusCode < 300) {
                if (successMsg != null) showAlert(Alert.AlertType.INFORMATION, "Thành công", successMsg);
                if (refreshCallback != null) refreshCallback.run(); // Load lại List
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Thao tác thất bại! Mã lỗi: " + statusCode);
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(msg);
        alert.showAndWait();
    }
}
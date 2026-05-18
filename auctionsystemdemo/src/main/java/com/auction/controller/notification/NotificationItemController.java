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

        // =======================================================
        // BỔ SUNG: NẾU LÀ YÊU CẦU KẾT BẠN THÌ CŨNG HIỆN 2 NÚT XANH ĐỎ
        // =======================================================
        if ("PAYMENT_VERIFICATION".equals(item.type) || "FRIEND_REQUEST".equals(item.type)) {
            btnAccept.setVisible(true); btnAccept.setManaged(true);
            btnDecline.setVisible(true); btnDecline.setManaged(true);
            btnDelete.setVisible(false); btnDelete.setManaged(false);
        } else {
            btnAccept.setVisible(false); btnAccept.setManaged(false);
            btnDecline.setVisible(false); btnDecline.setManaged(false);
            btnDelete.setVisible(true); btnDelete.setManaged(true);
        }
    }

    @FXML
    private void handleAccept() {
        String msg = "FRIEND_REQUEST".equals(currentItem.type) ? "Đã chấp nhận kết bạn!" : "Xác nhận thanh toán thành công!";
        ApiService.putAsync("/notifications/" + currentItem.notificationId + "/accept", null)
                .thenAccept(res -> handleResponse(res.statusCode(), msg));
    }

    @FXML
    private void handleDecline() {
        String msg = "FRIEND_REQUEST".equals(currentItem.type) ? "Đã từ chối kết bạn!" : "Đã từ chối thanh toán!";
        ApiService.putAsync("/notifications/" + currentItem.notificationId + "/decline", null)
                .thenAccept(res -> handleResponse(res.statusCode(), msg));
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
        com.auction.util.AlertUtils.applyStyle(alert);
        alert.showAndWait();
    }
}
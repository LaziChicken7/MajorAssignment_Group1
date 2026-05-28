package com.auction.controller.notification;

import com.auction.model.NotificationModel;
import com.auction.util.ApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;

import java.util.HashMap;
import java.util.Map;

@lombok.extern.slf4j.Slf4j
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
        // ĐIỀU KIỆN HIỆN NÚT DUYỆT (THÊM UPGRADE_REQUEST)
        // =======================================================
        if ("PAYMENT_VERIFICATION".equals(item.type) || "FRIEND_REQUEST".equals(item.type) || "UPGRADE_REQUEST".equals(item.type)) {
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
        log.info("\u25B6 Controller Action - Execute: handleAccept()");
        String tempMsg = "Xác nhận thành công!";
        if ("UPGRADE_REQUEST".equals(currentItem.type)) {
            tempMsg = "Đã phê duyệt yêu cầu lên Seller!";
        } else if ("FRIEND_REQUEST".equals(currentItem.type)) {
            tempMsg = "Đã chấp nhận kết bạn!";
        } else if ("PAYMENT_VERIFICATION".equals(currentItem.type)) {
            tempMsg = "Xác nhận thanh toán thành công!";
        }

        // CHỐT CHẶN: Gán vào biến final để Java cho phép dùng trong Lambda
        final String finalMsg = tempMsg;

        ApiService.putAsync("/notifications/" + currentItem.notificationId + "/accept", null)
                .thenAccept(res -> handleResponse(res.statusCode(), finalMsg));
    }

    @FXML
    private void handleDecline() {
        log.info("\u25B6 Controller Action - Execute: handleDecline()");
        // NẾU LÀ YÊU CẦU LÊN SELLER -> BẬT HỘP THOẠI HỎI LÝ DO
        if ("UPGRADE_REQUEST".equals(currentItem.type)) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Từ chối yêu cầu");
            dialog.setHeaderText("Từ chối cấp quyền Seller");
            dialog.setContentText("Nhập lý do từ chối (Sẽ gửi cho người dùng):");
            com.auction.util.AlertUtils.applyStyle(dialog); // CSS Dark/Light mode

            dialog.showAndWait().ifPresent(reason -> {
                if (reason.trim().isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Bạn bắt buộc phải nhập lý do từ chối!");
                    return;
                }

                // Gói lý do vào file JSON
                Map<String, String> body = new HashMap<>();
                body.put("reason", reason.trim());

                ApiService.putAsync("/notifications/" + currentItem.notificationId + "/decline", body)
                        .thenAccept(res -> handleResponse(res.statusCode(), "Đã gửi thông báo từ chối tới người dùng!"));
            });
        }
        // CÁC LOẠI TỪ CHỐI KHÁC (THANH TOÁN / KẾT BẠN) THÌ KHÔNG CẦN LÝ DO
        else {
            String msg = "FRIEND_REQUEST".equals(currentItem.type) ? "Đã từ chối kết bạn!" : "Đã từ chối thanh toán!";
            ApiService.putAsync("/notifications/" + currentItem.notificationId + "/decline", null)
                    .thenAccept(res -> handleResponse(res.statusCode(), msg));
        }
    }

    @FXML
    private void handleDelete() {
        log.info("\u25B6 Controller Action - Execute: handleDelete()");
        ApiService.deleteAsync("/notifications/" + currentItem.notificationId)
                .thenAccept(res -> handleResponse(res.statusCode(), null)); // Null msg để không nhảy Popup thông báo
    }

    // Hàm xử lý chung sau khi gọi API
    private void handleResponse(int statusCode, String successMsg) {
        log.info("\u25B6 Controller Action - Execute: handleResponse()");
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
        log.info("\u25B6 Controller Action - Execute: showAlert()");
        Alert alert = new Alert(type);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(msg);
        com.auction.util.AlertUtils.applyStyle(alert);
        alert.showAndWait();
    }
}
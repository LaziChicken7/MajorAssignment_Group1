package com.auction.controller.notification;

import com.auction.model.ApiResponse;
import com.auction.model.NotificationModel;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class NotificationController {

    @FXML private ListView<NotificationModel> lvNotifications;

    // --- CÁC THÀNH PHẦN CỦA POPUP ---
    @FXML private StackPane popupOverlay;
    @FXML private Label lblPopupTitle, lblPopupDesc, lblPopupTime;
    @FXML private Button btnPopupAccept, btnPopupDecline, btnPopupDelete, btnPopupClose;

    // Biến lưu trữ thông báo đang được click mở Popup
    private NotificationModel selectedNotification;

    @FXML
    public void initialize() {
        // Render từng dòng thông báo
        lvNotifications.setCellFactory(param -> new ListCell<NotificationModel>() {
            @Override
            protected void updateItem(NotificationModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/notification/NotificationItem.fxml"));
                        setGraphic(loader.load());

                        NotificationItemController controller = loader.getController();
                        controller.setData(item, () -> loadData());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // BẮT SỰ KIỆN CLICK VÀO 1 DÒNG ĐỂ MỞ POPUP
        lvNotifications.setOnMouseClicked(event -> {
            NotificationModel clickedItem = lvNotifications.getSelectionModel().getSelectedItem();
            if (clickedItem != null) {
                openPopup(clickedItem);
            }
        });

        loadData();
    }

    @FXML
    public void loadData() {
        if (SessionManager.userName == null) return;

        ApiService.getAsync("/notifications/" + SessionManager.userName).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        Type listType = new TypeToken<List<NotificationModel>>(){}.getType();
                        List<NotificationModel> list = ApiService.gson.fromJson(apiRes.result, listType);

                        ObservableList<NotificationModel> observableList = FXCollections.observableArrayList(list);
                        lvNotifications.setItems(observableList);

                        // =========================================================
                        // THÊM 3 DÒNG NÀY: ÉP BONG BÓNG TRANG CHÍNH CẬP NHẬT TỨC THÌ
                        // =========================================================
                        if (com.auction.controller.dashboard.MainController.getInstance() != null) {
                            com.auction.controller.dashboard.MainController.getInstance().updateNotificationCount(list != null ? list.size() : 0);
                        }
                    }
                }
            });
        });
    }

    // ==========================================
    // LOGIC ĐIỀU KHIỂN POPUP
    // ==========================================

    private void openPopup(NotificationModel item) {
        this.selectedNotification = item;

        lblPopupTitle.setText(item.title);
        lblPopupDesc.setText(item.description);
        lblPopupTime.setText(item.createdAt != null ? item.createdAt.replace("T", " ") : "");

        // =======================================================
        // BỔ SUNG FRIEND_REQUEST
        // =======================================================
        if ("PAYMENT_VERIFICATION".equals(item.type) || "FRIEND_REQUEST".equals(item.type)) {
            btnPopupAccept.setVisible(true); btnPopupAccept.setManaged(true);
            btnPopupDecline.setVisible(true); btnPopupDecline.setManaged(true);
            btnPopupDelete.setVisible(false); btnPopupDelete.setManaged(false);
            btnPopupClose.setVisible(true); btnPopupClose.setManaged(true);
        } else {
            btnPopupAccept.setVisible(false); btnPopupAccept.setManaged(false);
            btnPopupDecline.setVisible(false); btnPopupDecline.setManaged(false);
            btnPopupDelete.setVisible(true); btnPopupDelete.setManaged(true);
            btnPopupClose.setVisible(true); btnPopupClose.setManaged(true);
        }
        popupOverlay.setVisible(true);
    }

    @FXML
    private void closePopup() {
        popupOverlay.setVisible(false);
        selectedNotification = null;
    }

    // Xử lý khi ấn nút Xác nhận trên Popup
    @FXML
    private void handlePopupAccept() {
        if (selectedNotification == null) return;
        String msg = "FRIEND_REQUEST".equals(selectedNotification.type) ? "Đã chấp nhận kết bạn!" : "Xác nhận thanh toán thành công!";

        ApiService.putAsync("/notifications/" + selectedNotification.notificationId + "/accept", null)
                .thenAccept(res -> handleResponse(res.statusCode(), msg));
    }

    // Xử lý khi ấn nút Từ chối trên Popup
    @FXML
    private void handlePopupDecline() {
        if (selectedNotification == null) return;
        String msg = "FRIEND_REQUEST".equals(selectedNotification.type) ? "Đã từ chối kết bạn!" : "Đã từ chối thanh toán!";

        ApiService.putAsync("/notifications/" + selectedNotification.notificationId + "/decline", null)
                .thenAccept(res -> handleResponse(res.statusCode(), msg));
    }

    // Xử lý khi ấn nút Xóa trên Popup
    @FXML
    private void handlePopupDelete() {
        if (selectedNotification == null) return;
        ApiService.deleteAsync("/notifications/" + selectedNotification.notificationId)
                .thenAccept(res -> handleResponse(res.statusCode(), "Đã xóa thông báo!"));
    }

    @FXML
    private void handleDeleteAll() {
        if (SessionManager.userName == null) return;

        // Hiển thị hộp thoại xác nhận trước khi xóa
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận xóa");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Bạn có chắc chắn muốn xóa tất cả thông báo?\n(Các yêu cầu kết bạn và thanh toán sẽ được giữ lại)");
        com.auction.util.AlertUtils.applyStyle(confirmAlert); // Apply CSS nếu có

        // Chờ người dùng chọn OK hay Cancel
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Gọi API xóa tất cả
                ApiService.deleteAsync("/notifications/all/" + SessionManager.userName)
                        .thenAccept(res -> Platform.runLater(() -> {
                            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã dọn dẹp các thông báo cũ!");
                                loadData(); // Tải lại danh sách sau khi xóa
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Lỗi", "Thao tác thất bại! Mã lỗi: " + res.statusCode());
                            }
                        }));
            }
        });
    }

    private void handleResponse(int statusCode, String successMsg) {
        Platform.runLater(() -> {
            closePopup(); // Đóng popup sau khi bấm xong
            if (statusCode >= 200 && statusCode < 300) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", successMsg);
                loadData(); // Load lại danh sách
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
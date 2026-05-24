package com.auction.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class ToastNotification {

    // Khai báo các loại Toast để dễ dàng phân biệt Icon
    public enum ToastType {
        NOTIFICATION, // Thông báo chung (Hình chuông)
        CHAT          // Tin nhắn (Hình bong bóng chat)
    }

    private static final List<Popup> activeToasts = new ArrayList<>();
    private static final double TOAST_SPACING = 15.0;

    // =======================================================
    // 1. Hàm mặc định (Giữ nguyên cho các code cũ không bị lỗi)
    // =======================================================
    public static void show(String title, String message) {
        show(title, message, ToastType.NOTIFICATION, null);
    }

    // =======================================================
    // 2. Hàm có đổi Icon (Ví dụ: truyền vào ToastType.CHAT)
    // =======================================================
    public static void show(String title, String message, ToastType type) {
        show(title, message, type, null);
    }

    // =======================================================
    // 3. Hàm có Click chuột (Mặc định icon Chuông)
    // =======================================================
    public static void show(String title, String message, Runnable onClickAction) {
        show(title, message, ToastType.NOTIFICATION, onClickAction);
    }

    // =======================================================
    // 4. HÀM CHÍNH (Xử lý toàn bộ logic)
    // =======================================================
    public static void show(String title, String message, ToastType type, Runnable onClickAction) {
        Platform.runLater(() -> {

            Window mainWindow = Window.getWindows().stream()
                    .filter(Window::isShowing)
                    .findFirst()
                    .orElse(null);

            if (mainWindow == null) return;

            Popup popup = new Popup();
            popup.setAutoFix(false);

            VBox root = new VBox(5);
            boolean isDark = false;

            if (mainWindow.getScene() != null) {
                root.getStylesheets().addAll(mainWindow.getScene().getStylesheets());
                if (mainWindow.getScene().getRoot().getStyleClass().contains("dark-theme")) {
                    isDark = true;
                }
            }

            String bgColor = isDark ? "#1E1E1E" : "#FFFFFF";
            String borderColor = isDark ? "#2C2C2E" : "#ecf0f1";

            String rootStyle = "-fx-padding: 15; -fx-min-width: 320; -fx-max-width: 320; " +
                    "-fx-background-color: " + bgColor + "; " +
                    "-fx-background-radius: 10; -fx-border-radius: 10; " +
                    "-fx-border-color: " + borderColor + "; -fx-border-width: 1px; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 15, 0, 0, 5);";

            // BẮT SỰ KIỆN CLICK CHUỘT VÀO THÔNG BÁO
            if (onClickAction != null) {
                rootStyle += " -fx-cursor: hand;";
                root.setOnMouseClicked(e -> {
                    onClickAction.run(); // Chạy lệnh được truyền vào
                    removeToast(popup, root); // Ẩn Toast đi ngay lập tức cho mượt
                });
            }

            root.setStyle(rootStyle);

            // --- HEADER ---
            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);

            // LOGIC CHỌN ICON DỰA TRÊN TOAST TYPE
            SVGPath icon = new SVGPath();
            if (type == ToastType.CHAT) {
                // ICON BONG BÓNG CHAT
                icon.setContent("M 21 15 a 2 2 0 0 1 -2 2 H 7 l -4 4 V 5 a 2 2 0 0 1 2 -2 h 14 a 2 2 0 0 1 2 2 z");
                icon.setFill(Color.web(isDark ? "#34d399" : "#0984e3")); // Màu hơi hướng Messenger
            } else {
                // ICON CÁI CHUÔNG (MẶC ĐỊNH)
                icon.setContent("M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z");
                icon.setFill(Color.web(isDark ? "#60a5fa" : "#0A439D"));
            }
            icon.setScaleX(0.75); icon.setScaleY(0.75);

            Label lblTitle = new Label(title);
            String titleColor = isDark ? "#FFFFFF" : "#2c3e50";
            lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14.5px; -fx-padding: 0 0 0 5; -fx-text-fill: " + titleColor + ";");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button btnClose = new Button();
            SVGPath closeIcon = new SVGPath();
            closeIcon.setContent("M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z");
            closeIcon.setFill(Color.web(isDark ? "#A1A1AA" : "#95a5a6"));
            closeIcon.setScaleX(0.65); closeIcon.setScaleY(0.65);

            btnClose.setGraphic(closeIcon);
            btnClose.setStyle("-fx-background-color: transparent; -fx-background-radius: 50; -fx-cursor: hand; -fx-padding: 4;");

            boolean finalIsDark = isDark;
            btnClose.setOnMouseEntered(e -> {
                btnClose.setStyle("-fx-background-color: rgba(231, 76, 60, 0.15); -fx-background-radius: 50; -fx-cursor: hand; -fx-padding: 4;");
                closeIcon.setFill(Color.web("#e74c3c"));
            });
            btnClose.setOnMouseExited(e -> {
                btnClose.setStyle("-fx-background-color: transparent; -fx-background-radius: 50; -fx-cursor: hand; -fx-padding: 4;");
                closeIcon.setFill(Color.web(finalIsDark ? "#A1A1AA" : "#95a5a6"));
            });

            btnClose.setOnAction(e -> {
                e.consume(); // CHẶN LỖI: Bấm nút X thì không bị nhận nhầm là click vào màn hình Toast
                removeToast(popup, root);
            });

            header.getChildren().addAll(icon, lblTitle, spacer, btnClose);

            // --- BODY ---
            Label lblMessage = new Label(message);
            lblMessage.setWrapText(true);
            String msgColor = isDark ? "#E4E4E7" : "#576574";
            lblMessage.setStyle("-fx-font-size: 13.5px; -fx-line-spacing: 3px; -fx-text-fill: " + msgColor + ";");
            VBox.setMargin(lblMessage, new Insets(5, 0, 0, 25));

            root.getChildren().addAll(header, lblMessage);
            popup.getContent().add(root);

            root.setOpacity(0);
            popup.show(mainWindow);

            activeToasts.add(popup);

            Platform.runLater(() -> {
                double actualWidth = root.getWidth();
                double actualHeight = root.getHeight();

                double x = mainWindow.getX() + mainWindow.getWidth() - actualWidth - 30;

                double baseY = mainWindow.getY() + mainWindow.getHeight() - 30;
                double yOffset = 0;
                for (int i = 0; i < activeToasts.size() - 1; i++) {
                    yOffset += activeToasts.get(i).getContent().get(0).getBoundsInLocal().getHeight() + TOAST_SPACING;
                }

                double y = baseY - actualHeight - yOffset;

                popup.setX(x);
                popup.setY(y);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();

                PauseTransition delay = new PauseTransition(Duration.seconds(7));
                delay.setOnFinished(e -> removeToast(popup, root));
                delay.play();
            });
        });
    }

    private static void removeToast(Popup popup, VBox root) {
        if (!popup.isShowing()) return;

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> {
            popup.hide();
            activeToasts.remove(popup);
        });
        fadeOut.play();
    }
}
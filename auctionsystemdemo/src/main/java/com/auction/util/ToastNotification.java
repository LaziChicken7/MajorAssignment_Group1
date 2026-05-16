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

public class ToastNotification {

    public static void show(String title, String message) {
        Platform.runLater(() -> {

            // 1. Tìm cửa sổ app đang hiển thị
            Window mainWindow = Window.getWindows().stream()
                    .filter(Window::isShowing)
                    .findFirst()
                    .orElse(null);

            if (mainWindow == null) return;

            Popup popup = new Popup();
            // Tắt autoFix để hệ điều hành không tự ý xê dịch popup của chúng ta
            popup.setAutoFix(false);

            // 2. KHUNG CHÍNH (Giao diện)
            VBox root = new VBox(5);
            root.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                    "-fx-border-radius: 8; -fx-border-color: #ecf0f1; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 3); " +
                    "-fx-padding: 15; -fx-min-width: 320; -fx-max-width: 320;");

            // --- HEADER: ICON + TIÊU ĐỀ + NÚT X ---
            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);

            SVGPath chatIcon = new SVGPath();
            chatIcon.setContent("M 21 15 a 2 2 0 0 1 -2 2 H 7 l -4 4 V 5 a 2 2 0 0 1 2 -2 h 14 a 2 2 0 0 1 2 2 z");
            chatIcon.setFill(Color.web("#0A439D"));
            chatIcon.setScaleX(0.75); chatIcon.setScaleY(0.75);

            Label lblTitle = new Label(title);
            lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50; -fx-padding: 0 0 0 5;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button btnClose = new Button();
            SVGPath closeIcon = new SVGPath();
            closeIcon.setContent("M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z");
            closeIcon.setFill(Color.web("#95a5a6"));
            closeIcon.setScaleX(0.65); closeIcon.setScaleY(0.65);

            btnClose.setGraphic(closeIcon);
            btnClose.setStyle("-fx-background-color: transparent; -fx-background-radius: 50; -fx-cursor: hand; -fx-padding: 4;");

            btnClose.setOnMouseEntered(e -> {
                btnClose.setStyle("-fx-background-color: #ffeded; -fx-background-radius: 50; -fx-cursor: hand; -fx-padding: 4;");
                closeIcon.setFill(Color.web("#e74c3c"));
            });
            btnClose.setOnMouseExited(e -> {
                btnClose.setStyle("-fx-background-color: transparent; -fx-background-radius: 50; -fx-cursor: hand; -fx-padding: 4;");
                closeIcon.setFill(Color.web("#95a5a6"));
            });

            // Xử lý khi bấm nút X: Cho mờ dần rồi mới tắt hẳn
            btnClose.setOnAction(e -> {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(200), root);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(event -> popup.hide());
                fadeOut.play();
            });

            header.getChildren().addAll(chatIcon, lblTitle, spacer, btnClose);

            // --- BODY: NỘI DUNG TIN NHẮN ---
            Label lblMessage = new Label(message);
            lblMessage.setWrapText(true);
            lblMessage.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #576574;");
            VBox.setMargin(lblMessage, new Insets(2, 0, 0, 25));

            root.getChildren().addAll(header, lblMessage);
            popup.getContent().add(root);

            // =======================================================
            // FIX LỖI "LỌT THỎM":
            // B1: Cho tàng hình (Opacity = 0) và show lên trước
            // =======================================================
            root.setOpacity(0);
            popup.show(mainWindow);

            // B2: Gọi thêm một luồng để canh kích thước thực tế sau khi giao diện render xong
            Platform.runLater(() -> {
                double actualWidth = root.getWidth();
                double actualHeight = root.getHeight();

                // Tính toán chính xác mép dưới (cách 30px) và mép phải (cách 30px)
                double x = mainWindow.getX() + mainWindow.getWidth() - actualWidth - 30;
                double y = mainWindow.getY() + mainWindow.getHeight() - actualHeight - 30;

                popup.setX(x);
                popup.setY(y);

                // B3: Hiệu ứng Animation mờ dần hiện ra
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();

                // B4: Đếm ngược 10 giây tự động mờ dần rồi tắt
                PauseTransition delay = new PauseTransition(Duration.seconds(10));
                delay.setOnFinished(e -> {
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root);
                    fadeOut.setFromValue(1);
                    fadeOut.setToValue(0);
                    fadeOut.setOnFinished(event -> popup.hide());
                    fadeOut.play();
                });
                delay.play();
            });
        });
    }
}
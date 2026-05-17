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

            Window mainWindow = Window.getWindows().stream()
                    .filter(Window::isShowing)
                    .findFirst()
                    .orElse(null);

            if (mainWindow == null) return;

            Popup popup = new Popup();
            popup.setAutoFix(false);

            VBox root = new VBox(5);
            boolean isDark = false;

            // KÉO CSS VÀ KÍCH HOẠT DARK MODE
            if (mainWindow.getScene() != null) {
                root.getStylesheets().addAll(mainWindow.getScene().getStylesheets());
                if (mainWindow.getScene().getRoot().getStyleClass().contains("dark-theme")) {
                    isDark = true;
                }
            }

            // ========================================================
            // ÉP MÀU NỀN VÀ VIỀN TRỰC TIẾP ĐỂ KHÔNG BỊ LỖI KẾT THỪA CSS
            // ========================================================
            String bgColor = isDark ? "#1E1E1E" : "#FFFFFF";
            String borderColor = isDark ? "#2C2C2E" : "#ecf0f1";

            root.setStyle("-fx-padding: 15; -fx-min-width: 320; -fx-max-width: 320; " +
                    "-fx-background-color: " + bgColor + "; " +
                    "-fx-background-radius: 10; -fx-border-radius: 10; " +
                    "-fx-border-color: " + borderColor + "; -fx-border-width: 1px; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 15, 0, 0, 5);");

            // --- HEADER: ICON + TIÊU ĐỀ + NÚT X ---
            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);

            SVGPath chatIcon = new SVGPath();
            chatIcon.setContent("M 21 15 a 2 2 0 0 1 -2 2 H 7 l -4 4 V 5 a 2 2 0 0 1 2 -2 h 14 a 2 2 0 0 1 2 2 z");
            // Ép màu Icon: Sáng thì Xanh đậm, Tối thì Xanh da trời cho nổi bật
            chatIcon.setFill(Color.web(isDark ? "#60a5fa" : "#0A439D"));
            chatIcon.setScaleX(0.75); chatIcon.setScaleY(0.75);

            Label lblTitle = new Label(title);
            // Ép màu Tiêu đề: Sáng thì Đen, Tối thì Trắng tinh
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
            // Ép màu Nội dung: Sáng thì Xám đậm, Tối thì Xám sáng (Trắng xám)
            String msgColor = isDark ? "#E4E4E7" : "#576574";
            lblMessage.setStyle("-fx-font-size: 13.5px; -fx-line-spacing: 3px; -fx-text-fill: " + msgColor + ";");
            VBox.setMargin(lblMessage, new Insets(5, 0, 0, 25));

            root.getChildren().addAll(header, lblMessage);
            popup.getContent().add(root);

            // =======================================================
            // FIX LỖI "LỌT THỎM":
            // =======================================================
            root.setOpacity(0);
            popup.show(mainWindow);

            Platform.runLater(() -> {
                double actualWidth = root.getWidth();
                double actualHeight = root.getHeight();

                double x = mainWindow.getX() + mainWindow.getWidth() - actualWidth - 30;
                double y = mainWindow.getY() + mainWindow.getHeight() - actualHeight - 30;

                popup.setX(x);
                popup.setY(y);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();

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
package com.auction.controller;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Label lblSystemTime;

    // Khai báo 2 thành phần của Sidebar để làm hiệu ứng
    @FXML private AnchorPane drawerOverlay;
    @FXML private VBox expandedSidebar;

    @FXML
    public void initialize() {
        showDashboard();
        startClock();
    }

    private void startClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            if (lblSystemTime != null) {
                lblSystemTime.setText(LocalDateTime.now().format(formatter));
            }
        }), new KeyFrame(Duration.seconds(1)));

        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    public void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML public void showDashboard() { loadView("/com/auction/view/Dashboard.fxml"); }
    @FXML public void showAuctionList() { loadView("/com/auction/view/AuctionList.fxml"); }
    @FXML public void handleShowMyProducts() { loadView("/com/auction/view/MyAuctionList.fxml"); }
    @FXML public void showNotification() { loadView("/com/auction/view/NotificationList.fxml"); }
    @FXML public void showProfile() { loadView("/com/auction/view/Profile.fxml"); }
    @FXML public void showWallet() { loadView("/com/auction/view/Wallet.fxml"); }

    // ====== HÀM XỬ LÝ HIỆU ỨNG SIDEBAR ======

    @FXML
    public void openSidebar() {
        if (expandedSidebar != null && drawerOverlay != null) {
            // 1. Hiện lớp nền đen
            drawerOverlay.setVisible(true);

            // 2. Hiệu ứng trượt Sidebar từ trái sang phải (kéo ra 280px)
            TranslateTransition slide = new TranslateTransition(Duration.millis(300), expandedSidebar);
            slide.setToX(280);

            // 3. Hiệu ứng mờ dần lớp nền đen từ 0% lên 100%
            FadeTransition fade = new FadeTransition(Duration.millis(300), drawerOverlay);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);

            // 4. Chạy cả 2 hiệu ứng cùng lúc
            ParallelTransition pt = new ParallelTransition(slide, fade);
            pt.play();
        }
    }

    @FXML
    public void closeSidebar() {
        if (expandedSidebar != null && drawerOverlay != null) {
            // 1. Hiệu ứng trượt Sidebar thụt lùi về vị trí cũ (về 0)
            TranslateTransition slide = new TranslateTransition(Duration.millis(300), expandedSidebar);
            slide.setToX(0);

            // 2. Hiệu ứng lớp nền mờ dần đi về 0%
            FadeTransition fade = new FadeTransition(Duration.millis(300), drawerOverlay);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);

            // 3. Chạy cả 2 hiệu ứng cùng lúc
            ParallelTransition pt = new ParallelTransition(slide, fade);

            // SAU KHI hiệu ứng kết thúc -> Ẩn hoàn toàn lớp nền đen để không che nút bấm ở dưới
            pt.setOnFinished(e -> drawerOverlay.setVisible(false));

            pt.play();
        }
    }
}
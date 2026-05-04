package com.auction.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label; // SỬA LỖI: javafx chứ không phải java.lang
import javafx.scene.layout.StackPane;
import javafx.util.Duration; // SỬA LỖI: javafx.util chứ không phải java.time
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Label lblSystemTime; // Đảm bảo fx:id trong Main.fxml là lblSystemTime

    @FXML
    public void initialize() {
        // GỘP CẢ 2 HÀM INITIALIZE LÀM 1 ĐỂ HẾT LỖI "Already defined"
        showDashboard();
        startClock();
    }

    private void startClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        // Sử dụng javafx.util.Duration.seconds(1)
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

    // ====== HÀM XỬ LÝ SIDEBAR (Sửa lỗi Error resolving onAction='#openSidebar') ======

    @FXML
    public void openSidebar() {
        System.out.println("Đang mở Sidebar...");
        // Nếu bạn có viết hiệu ứng chạy (Animation) thì dán vào đây
        // Tạm thời nếu chỉ muốn hiện ra:
        /*
        if (expandedSidebar != null && drawerOverlay != null) {
            drawerOverlay.setVisible(true);
            expandedSidebar.setTranslateX(280);
        }
        */
    }

    @FXML
    public void closeSidebar() {
        System.out.println("Đang đóng Sidebar...");
        // Nếu bạn có viết hiệu ứng chạy (Animation) thì dán vào đây
        /*
        if (expandedSidebar != null && drawerOverlay != null) {
            expandedSidebar.setTranslateX(0);
            drawerOverlay.setVisible(false);
        }
        */
    }
}
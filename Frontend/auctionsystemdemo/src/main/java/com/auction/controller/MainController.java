package com.auction.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class MainController {

    // Phải khớp với fx:id="contentArea" trong Main.fxml
    @FXML private StackPane contentArea;

    @FXML
    public void initialize() {
        // Khi mở app, hiển thị trang Dashboard đầu tiên
        showDashboard();
    }

    // Hàm nạp trang con vào vùng giữa
    public void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
            }
        } catch (IOException e) {
            System.err.println("Không tìm thấy file FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }

    // ====== CÁC HÀM XỬ LÝ SỰ KIỆN TỪ SIDEBAR (SỬA LỖI CỦA BẠN TẠI ĐÂY) ======

    @FXML
    public void showDashboard() {
        loadView("/com/auction/view/Dashboard.fxml");
    }

    @FXML
    public void showAuctionList() {
        loadView("/com/auction/view/AuctionList.fxml");
    }

    @FXML
    public void handleShowMyProducts() {
        loadView("/com/auction/view/MyAuctionList.fxml");
    }

    @FXML
    public void showWallet() {
        // Nếu chưa có file Wallet.fxml, hãy tạo file trống hoặc tạm thời comment loadView
        loadView("/com/auction/view/Wallet.fxml");
    }

    @FXML
    public void showProfile() {
        loadView("/com/auction/view/Profile.fxml");
    }

    @FXML
    public void showNotification() {
        loadView("/com/auction/view/Notification.fxml");
    }

    @FXML public void openSidebar() {}
    @FXML public void closeSidebar() {}
}
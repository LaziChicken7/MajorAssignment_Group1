package com.auction.controller;

import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private AnchorPane drawerOverlay;
    @FXML private VBox expandedSidebar;

    // Các nút bên Sidebar thu gọn
    @FXML private Button btnIconHome, btnIconWallet, btnIconAuction, btnIconAdd, btnIconNotif, btnIconProfile;
    // Các nút bên Sidebar mở rộng
    @FXML private Button btnNavHome, btnNavWallet, btnNavAuction, btnNavAdd, btnNavNotif, btnNavProfile;

    @FXML
    public void initialize() {
        showDashboard(); // Khi vừa bật app thì vào Trang chủ đầu tiên
    }

    // ====== HÀM XỬ LÝ HIGHLIGHT MÀU NÚT ======
    private void setActiveMenu(Button activeIcon, Button activeNav) {
        // Danh sách tất cả các nút để tiện reset
        Button[] allIcons = {btnIconHome, btnIconWallet, btnIconAuction, btnIconAdd, btnIconNotif, btnIconProfile};
        Button[] allNavs = {btnNavHome, btnNavWallet, btnNavAuction, btnNavAdd, btnNavNotif, btnNavProfile};

        // 1. Reset toàn bộ về trạng thái bình thường (trong suốt)
        for (Button btn : allIcons) {
            if (btn != null) {
                btn.getStyleClass().remove("icon-btn-active");
                if (!btn.getStyleClass().contains("icon-btn")) btn.getStyleClass().add("icon-btn");
            }
        }
        for (Button btn : allNavs) {
            if (btn != null) {
                btn.getStyleClass().remove("nav-btn-active");
                if (!btn.getStyleClass().contains("nav-btn")) btn.getStyleClass().add("nav-btn");
            }
        }

        // 2. Add class active (Màu xanh đậm) cho nút vừa được chọn
        if (activeIcon != null) {
            activeIcon.getStyleClass().remove("icon-btn");
            activeIcon.getStyleClass().add("icon-btn-active");
        }
        if (activeNav != null) {
            activeNav.getStyleClass().remove("nav-btn");
            activeNav.getStyleClass().add("nav-btn-active");
        }
    }

    // ====== ANIMATION MỞ/ĐÓNG SIDEBAR ======
    @FXML
    public void openSidebar() {
        drawerOverlay.setVisible(true);
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), expandedSidebar);
        slide.setToX(280);
        slide.play();
    }

    @FXML
    public void closeSidebar() {
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), expandedSidebar);
        slide.setToX(0);
        slide.setOnFinished(e -> drawerOverlay.setVisible(false));
        slide.play();
    }

    // ====== CÁC HÀM CHUYỂN TRANG ======
    @FXML public void showDashboard() {
        loadView("/com/auction/view/Dashboard.fxml");
        setActiveMenu(btnIconHome, btnNavHome); // Gọi hàm tô đậm nút Home
        closeSidebar();
    }

    @FXML public void showWallet() {
        loadView("/com/auction/view/Wallet.fxml");
        setActiveMenu(btnIconWallet, btnNavWallet); // Gọi hàm tô đậm nút Wallet
        closeSidebar();
    }

    @FXML public void showAuctionList() {
        loadView("/com/auction/view/AuctionList.fxml");
        setActiveMenu(btnIconAuction, btnNavAuction);
        closeSidebar();
    }

    @FXML public void showAddProduct() {
        loadView("/com/auction/view/AddProduct.fxml");
        setActiveMenu(btnIconAdd, btnNavAdd);
        closeSidebar();
    }

    @FXML public void showProfile() {
        loadView("/com/auction/view/Profile.fxml");
        setActiveMenu(btnIconProfile, btnNavProfile);
        closeSidebar();
    }

    private void loadView(String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) return;
            FXMLLoader loader = new FXMLLoader(url);
            Node view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
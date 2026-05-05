package com.auction.controller;

import com.auction.util.SessionManager;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Label lblSystemTime;

    @FXML private AnchorPane drawerOverlay;
    @FXML private VBox expandedSidebar;

    // --- CÁC NÚT SIDEBAR THU GỌN (ICON) ---
    @FXML private Button btnIconHome;
    @FXML private Button btnIconWallet;
    @FXML private Button btnIconAuction;
    @FXML private Button btnIconAdd;
    @FXML private Button btnIconNotif;
    @FXML private Button btnIconProfile;

    // --- CÁC NÚT SIDEBAR MỞ RỘNG (TEXT) ---
    @FXML private Button btnNavHome;
    @FXML private Button btnNavWallet;
    @FXML private Button btnNavAuction;
    @FXML private Button btnNavAdd;
    @FXML private Button btnNavNotif;
    @FXML private Button btnNavProfile; // Dùng nút này để hiển thị tên thay cho Label

    private List<Button> allMenuButtons;

    @FXML
    public void initialize() {
        // Gom tất cả nút vào 1 danh sách để dễ dàng reset màu sắc
        allMenuButtons = Arrays.asList(
                btnIconHome, btnIconWallet, btnIconAuction, btnIconAdd, btnIconNotif, btnIconProfile,
                btnNavHome, btnNavWallet, btnNavAuction, btnNavAdd, btnNavNotif, btnNavProfile
        );

        loadUserInfo();
        showDashboard(null); // Mở trang chủ mặc định ban đầu
        startClock();
    }

    // Hiển thị tên đăng nhập lên nút Profile ở sidebar mở rộng
    private void loadUserInfo() {
        if (btnNavProfile != null && SessionManager.userName != null) {
            btnNavProfile.setText("👤   " + SessionManager.userName);
        }
    }

    private void startClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            if (lblSystemTime != null) lblSystemTime.setText(LocalDateTime.now().format(formatter));
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

    // Hàm tô màu nút được chọn (xóa màu nút cũ, thêm màu cho nút icon & text tương ứng)
    private void setActiveButton(Button iconBtn, Button navBtn) {
        // Xóa class active ở tất cả các nút
        for (Button btn : allMenuButtons) {
            if (btn != null) btn.getStyleClass().remove("active-menu-btn");
        }
        // Thêm class active vào nút hiện tại (cả lúc thu gọn lẫn mở rộng)
        if (iconBtn != null) iconBtn.getStyleClass().add("active-menu-btn");
        if (navBtn != null) navBtn.getStyleClass().add("active-menu-btn");
    }

    // ====== CÁC HÀM XỬ LÝ CHUYỂN TRANG ======

    @FXML public void showDashboard(ActionEvent event) {
        loadView("/com/auction/view/Dashboard.fxml");
        setActiveButton(btnIconHome, btnNavHome);
        closeSidebar();
    }

    @FXML public void showWallet(ActionEvent event) {
        loadView("/com/auction/view/Wallet.fxml");
        setActiveButton(btnIconWallet, btnNavWallet);
        closeSidebar();
    }

    @FXML public void showAuctionList(ActionEvent event) {
        loadView("/com/auction/view/AuctionList.fxml");
        setActiveButton(btnIconAuction, btnNavAuction);
        closeSidebar();
    }

    @FXML public void handleShowMyProducts(ActionEvent event) {
        loadView("/com/auction/view/MyAuctionList.fxml");
        setActiveButton(btnIconAdd, btnNavAdd);
        closeSidebar();
    }

    @FXML public void showNotification(ActionEvent event) {
        loadView("/com/auction/view/NotificationList.fxml");
        setActiveButton(btnIconNotif, btnNavNotif);
        closeSidebar();
    }

    @FXML public void showProfile(ActionEvent event) {
        loadView("/com/auction/view/Profile.fxml");
        setActiveButton(btnIconProfile, btnNavProfile);
        closeSidebar();
    }

    // ====== HÀM XỬ LÝ HIỆU ỨNG SIDEBAR ======

    @FXML
    public void openSidebar() {
        if (expandedSidebar != null && drawerOverlay != null) {
            drawerOverlay.setVisible(true);
            TranslateTransition slide = new TranslateTransition(Duration.millis(300), expandedSidebar);
            slide.setToX(280);
            FadeTransition fade = new FadeTransition(Duration.millis(300), drawerOverlay);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            new ParallelTransition(slide, fade).play();
        }
    }

    @FXML
    public void closeSidebar() {
        if (expandedSidebar != null && drawerOverlay != null) {
            TranslateTransition slide = new TranslateTransition(Duration.millis(300), expandedSidebar);
            slide.setToX(0);
            FadeTransition fade = new FadeTransition(Duration.millis(300), drawerOverlay);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            ParallelTransition pt = new ParallelTransition(slide, fade);
            pt.setOnFinished(e -> drawerOverlay.setVisible(false));
            pt.play();
        }
    }
}
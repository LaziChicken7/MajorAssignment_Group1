package com.auction.controller;

import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
    @FXML private Label lblTime;
    @FXML private Label lblDate;

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

    private Timeline banCheckerTimeline;

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
        startBanChecker();
    }

    // Hiển thị tên đăng nhập lên nút Profile ở sidebar mở rộng
    private void loadUserInfo() {
        if (btnNavProfile != null && SessionManager.userName != null) {
            btnNavProfile.setText("👤   " + SessionManager.userName);
        }
    }

    private void startClock() {
        // Định dạng Giờ và Ngày (Tiếng Việt)
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", new java.util.Locale("vi", "VN"));

        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            LocalDateTime now = LocalDateTime.now();

            if (lblTime != null) {
                lblTime.setText(now.format(timeFormatter));
            }

            if (lblDate != null) {
                String dateStr = now.format(dateFormatter);
                lblDate.setText(dateStr.substring(0, 1).toUpperCase() + dateStr.substring(1));
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

    // ==========================================
    // HỆ THỐNG QUÉT TÀI KHOẢN BỊ BAN NGẦM (MỖI 5 GIÂY)
    // ==========================================
    private void startBanChecker() {
        // Admin thì không bao giờ bị khóa, nên bỏ qua không quét để tiết kiệm tài nguyên
        if (SessionManager.userName == null || "ADMIN".equals(SessionManager.role)) return;

        banCheckerTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {

            // Nếu người dùng đã tự đăng xuất, dừng quét luôn
            if (SessionManager.userName == null) {
                banCheckerTimeline.stop();
                return;
            }

            // Gọi nhẹ API Profile để kiểm tra
            ApiService.getAsync("/users/profile/" + SessionManager.userName).thenAccept(res -> {
                Platform.runLater(() -> {
                    if (res.statusCode() == 200) {
                        com.auction.model.ApiResponse apiRes = ApiService.gson.fromJson(res.body(), com.auction.model.ApiResponse.class);
                        if (apiRes.code == 1000) {
                            com.auction.model.UserProfile profile = ApiService.gson.fromJson(apiRes.result, com.auction.model.UserProfile.class);

                            // NẾU PHÁT HIỆN BỊ BAN (Khóa tài khoản) -> ĐÁ VĂNG NGAY LẬP TỨC
                            if (profile.banned) {
                                forceLogoutBannedUser();
                            }
                        }
                    }
                });
            });
        }));
        banCheckerTimeline.setCycleCount(Timeline.INDEFINITE); // Chạy vô hạn
        banCheckerTimeline.play();
    }

    private void forceLogoutBannedUser() {
        // 1. Dừng bộ quét
        if (banCheckerTimeline != null) banCheckerTimeline.stop();

        // 2. Xóa session
        SessionManager.logout();

        // 3. Hiển thị thông báo
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Thông báo khẩn cấp");
        alert.setHeaderText("TÀI KHOẢN ĐÃ BỊ KHÓA!");
        alert.setContentText("Tài khoản của bạn vừa bị Admin khóa do vi phạm.\nBạn sẽ bị đăng xuất ngay lập tức.");

        // =========================================================
        // THÊM CODE ĐỂ PHÓNG TO MÀN HÌNH ALERT
        // =========================================================
        // 1. Ép kích thước khung to ra (Rộng 450, Cao 250)
        alert.getDialogPane().setPrefSize(450, 250);

        // 2. Phóng to cỡ chữ bên trong để cân xứng với khung
        alert.getDialogPane().setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        // =========================================================

        alert.showAndWait();

        // 4. Chuyển về màn hình đăng nhập
        try {
            javafx.scene.Parent root = FXMLLoader.load(getClass().getResource("/com/auction/view/Login.fxml"));
            javafx.stage.Stage stage = (javafx.stage.Stage) contentArea.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.centerOnScreen();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
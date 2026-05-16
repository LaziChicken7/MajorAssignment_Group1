package com.auction.controller.dashboard;

import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class MainController {

    private static MainController instance;
    public MainController() { instance = this; }
    public static MainController getInstance() { return instance; }

    @FXML private StackPane rootPane;
    @FXML private StackPane contentArea;
    @FXML private Label lblTime, lblDate;
    @FXML private TextField txtSearch;

    // --- DARK MODE SWITCH ---
    @FXML private Rectangle switchBackground;
    @FXML private Circle switchKnob;
    private boolean isDarkMode = false;

    // --- SIDEBAR PUSH EFFECT ---
    @FXML private VBox sidebar;
    @FXML private SVGPath iconToggle;
    private boolean isSidebarExpanded = false;

    // --- CÁC NÚT MENU CHÍNH ---
    @FXML private Button btnNavHome, btnNavWallet, btnNavAuction, btnNavAdd, btnNavNotif, btnNavProfile, btnNavChat;
    private List<Button> allMenuButtons;
    private Timeline banCheckerTimeline;

    @FXML
    public void initialize() {
        allMenuButtons = Arrays.asList(btnNavHome, btnNavWallet, btnNavAuction, btnNavAdd, btnNavNotif, btnNavProfile, btnNavChat);

        // ĐỂ NGUYÊN HIỂN THỊ TRÁI VÀ BẬT CHẾ ĐỘ CẮT CHỮ (CLIP)
        for (Button btn : allMenuButtons) {
            if (btn != null) {
                btn.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
                btn.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP); // Phép thuật nằm ở đây!
            }
        }

        // Cắt khung mượt mà
        Rectangle clipRect = new Rectangle();
        clipRect.widthProperty().bind(sidebar.widthProperty());
        clipRect.heightProperty().bind(sidebar.heightProperty());
        sidebar.setClip(clipRect);

        loadUserInfo();
        showDashboard(null);
        startClock();
        startBanChecker();
    }

    // Đổ dữ liệu Avatar
    public void loadUserInfo() {
        if (btnNavProfile == null || SessionManager.userName == null) return;
        btnNavProfile.setText(SessionManager.userName);

        ApiService.getAsync("/users/profile/" + SessionManager.userName).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    com.auction.model.ApiResponse apiRes = ApiService.gson.fromJson(res.body(), com.auction.model.ApiResponse.class);
                    if (apiRes.code == 1000) {
                        com.auction.model.UserProfile profile = ApiService.gson.fromJson(apiRes.result, com.auction.model.UserProfile.class);

                        String avatarPath = profile.avatarUrl;
                        if (avatarPath == null || !avatarPath.startsWith("/uploads") || avatarPath.contains("default-")) {
                            avatarPath = "/uploads/images/avatar/avatarmacdinh.png";
                        }

                        String fullImageUrl = ApiService.BASE_URL + avatarPath + "?t=" + System.currentTimeMillis();
                        // Thu nhỏ Avatar cho bằng với Icon nét viền (Bán kính 18 -> Đường kính 36px)
                        ImageView avatar = createCircularAvatar(fullImageUrl, 18);
                        btnNavProfile.setGraphic(avatar);
                    }
                }
            });
        });
    }

    private void startClock() {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", new java.util.Locale("vi", "VN"));

        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            LocalDateTime now = LocalDateTime.now();
            if (lblTime != null) lblTime.setText(now.format(timeFormatter));
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
            // ==========================================================
            // THÊM 2 DÒNG NÀY: RESET CHAT KHI CHUYỂN BẤT KỲ TRANG NÀO
            // ==========================================================
            com.auction.util.GlobalWebSocketManager.currentActiveChatPartner = null;
            com.auction.util.GlobalWebSocketManager.setActiveChatListener(null);
            // ==========================================================

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void setActiveButton(Button navBtn) {
        for (Button btn : allMenuButtons) if (btn != null) btn.getStyleClass().remove("active-menu-btn");
        if (navBtn != null) navBtn.getStyleClass().add("active-menu-btn");
    }

    // ====== CÁC HÀM XỬ LÝ CHUYỂN TRANG ======
    @FXML public void showDashboard(ActionEvent event) { loadView("/com/auction/view/dashboard/Dashboard.fxml"); setActiveButton(btnNavHome); collapseSidebar(); }
    @FXML public void showWallet(ActionEvent event) { loadView("/com/auction/view/wallet/Wallet.fxml"); setActiveButton(btnNavWallet); collapseSidebar(); }
    @FXML public void showAuctionList(ActionEvent event) { loadView("/com/auction/view/auction/AuctionList.fxml"); setActiveButton(btnNavAuction); collapseSidebar(); }
    @FXML public void handleShowMyProducts(ActionEvent event) { loadView("/com/auction/view/auction/MyAuctionList.fxml"); setActiveButton(btnNavAdd); collapseSidebar(); }
    @FXML public void showNotification(ActionEvent event) { loadView("/com/auction/view/notification/NotificationList.fxml"); setActiveButton(btnNavNotif); collapseSidebar(); }
    @FXML public void showProfile(ActionEvent event) { loadView("/com/auction/view/profile/Profile.fxml"); setActiveButton(btnNavProfile); collapseSidebar(); }
    @FXML public void showChat(ActionEvent event) { loadView("/com/auction/view/chat/Chat.fxml"); setActiveButton(btnNavChat); collapseSidebar(); }

    // ==========================================
    // ANIMATION: HIỆU ỨNG PUSH MỞ RỘNG SIDEBAR
    // ==========================================
    @FXML
    public void toggleSidebar() {
        if (isSidebarExpanded) collapseSidebar();
        else expandSidebar();
    }

    private void expandSidebar() {
        if (!isSidebarExpanded) {
            isSidebarExpanded = true;
            iconToggle.setContent("M 18 6 L 6 18 M 6 6 L 18 18");

            // Đã xóa vòng lặp for thay đổi ContentDisplay ở đây

            animateSidebarWidth(280);
        }
    }

    private void collapseSidebar() {
        if (isSidebarExpanded) {
            isSidebarExpanded = false;
            iconToggle.setContent("M 3 12 h 18 M 3 6 h 18 M 3 18 h 18");

            // Đã xóa vòng lặp for thay đổi ContentDisplay ở đây

            animateSidebarWidth(110);
        }
    }

    private void animateSidebarWidth(double targetWidth) {
        Timeline timeline = new Timeline();
        KeyValue kvPref = new KeyValue(sidebar.prefWidthProperty(), targetWidth, Interpolator.EASE_BOTH);
        KeyValue kvMin = new KeyValue(sidebar.minWidthProperty(), targetWidth, Interpolator.EASE_BOTH);
        KeyValue kvMax = new KeyValue(sidebar.maxWidthProperty(), targetWidth, Interpolator.EASE_BOTH);

        KeyFrame kf = new KeyFrame(Duration.millis(250), kvPref, kvMin, kvMax);
        timeline.getKeyFrames().add(kf);
        timeline.play();
    }

    // ==========================================
    // XỬ LÝ SỰ KIỆN GẠT DARK MODE
    // ==========================================
    @FXML
    public void handleThemeToggle() {
        isDarkMode = !isDarkMode;
        TranslateTransition transition = new TranslateTransition(Duration.millis(250), switchKnob);

        if (isDarkMode) {
            transition.setToX(10);
            switchBackground.setFill(Color.web("#2c3e50"));
            rootPane.getStyleClass().add("dark-theme");
        } else {
            transition.setToX(-10);
            switchBackground.setFill(Color.web("#bdc3c7"));
            rootPane.getStyleClass().remove("dark-theme");
        }
        transition.play();
    }

    private void startBanChecker() {
        if (SessionManager.userName == null || "ADMIN".equals(SessionManager.role)) return;

        banCheckerTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            if (SessionManager.userName == null) { banCheckerTimeline.stop(); return; }

            ApiService.getAsync("/users/profile/" + SessionManager.userName).thenAccept(res -> {
                Platform.runLater(() -> {
                    if (res.statusCode() == 200) {
                        com.auction.model.ApiResponse apiRes = ApiService.gson.fromJson(res.body(), com.auction.model.ApiResponse.class);
                        if (apiRes.code == 1000) {
                            com.auction.model.UserProfile profile = ApiService.gson.fromJson(apiRes.result, com.auction.model.UserProfile.class);
                            if (profile.banned) forceLogoutBannedUser();
                        }
                    }
                });
            });
        }));
        banCheckerTimeline.setCycleCount(Timeline.INDEFINITE);
        banCheckerTimeline.play();
    }

    private ImageView createCircularAvatar(String imageUrl, double radius) {
        Image image = new Image(imageUrl, radius * 2, radius * 2, true, true, true);
        ImageView imageView = new ImageView(image);
        Circle clip = new Circle(radius, radius, radius);
        imageView.setClip(clip);
        return imageView;
    }

    private void forceLogoutBannedUser() {
        if (banCheckerTimeline != null) banCheckerTimeline.stop();
        SessionManager.logout();

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Thông báo khẩn cấp");
        alert.setHeaderText("TÀI KHOẢN ĐÃ BỊ KHÓA!");
        alert.setContentText("Tài khoản của bạn vừa bị Admin khóa do vi phạm.\nBạn sẽ bị đăng xuất ngay lập tức.");
        alert.getDialogPane().setPrefSize(450, 250);
        alert.getDialogPane().setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        alert.showAndWait();

        try {
            javafx.scene.Parent root = FXMLLoader.load(getClass().getResource("/com/auction/view/dashboard/Login.fxml"));
            javafx.stage.Stage stage = (javafx.stage.Stage) contentArea.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.centerOnScreen();
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    @FXML
    public void handleSearch() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/search/Search.fxml"));
            Node view = loader.load();

            com.auction.controller.search.SearchController controller = loader.getController();
            controller.executeSearch(keyword);

            for (Button btn : allMenuButtons) if (btn != null) btn.getStyleClass().remove("active-menu-btn");

            contentArea.getChildren().setAll(view);
            collapseSidebar();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
package com.auction.controller.dashboard;

import com.auction.controller.chat.ChatController;
import com.auction.controller.notification.NotificationController;
import com.auction.model.NotificationModel;
import com.auction.util.ApiService;
import com.auction.util.GlobalWebSocketManager;
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

    // --- SIDEBAR PUSH EFFECT ---
    @FXML private VBox sidebar;
    @FXML private SVGPath iconToggle;
    private boolean isSidebarExpanded = false;

    // --- CÁC NÚT MENU CHÍNH ---
    @FXML private Button btnNavHome, btnNavWallet, btnNavAuction, btnNavAdd, btnNavNotif, btnNavProfile, btnNavChat;
    private List<Button> allMenuButtons;

    private Timeline banCheckerTimeline;
    private Timeline notifCheckerTimeline; // Luồng kiểm tra thông báo realtime

    @FXML private Label lblNotifBadge;

    @FXML private Label lblChatBadge;

    // BIẾN QUAN TRỌNG: Lưu lại màn hình đang hiển thị và số lượng thông báo cũ
    private Object currentActiveController;
    private int currentNotifCount = -1;

    @FXML
    public void initialize() {
        allMenuButtons = Arrays.asList(btnNavHome, btnNavWallet, btnNavAuction, btnNavAdd, btnNavNotif, btnNavProfile, btnNavChat);

        for (Button btn : allMenuButtons) {
            if (btn != null) {
                btn.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
                btn.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
            }
        }

        Rectangle clipRect = new Rectangle();
        clipRect.widthProperty().bind(sidebar.widthProperty());
        clipRect.heightProperty().bind(sidebar.heightProperty());
        sidebar.setClip(clipRect);

        loadUserInfo();
        showDashboard(null);
        startClock();

        startBanChecker();
        startNotificationChecker(); // BẬT HỆ THỐNG QUÉT THÔNG BÁO REAL-TIME

        GlobalWebSocketManager.initConnection();

        applyCurrentTheme(false);
    }

    @FXML
    public void handleThemeToggle() {
        SessionManager.isDarkMode = !SessionManager.isDarkMode;
        applyCurrentTheme(true);
    }

    private void applyCurrentTheme(boolean animate) {
        if (SessionManager.isDarkMode) {
            switchBackground.setFill(Color.web("#2c3e50"));
            if (!rootPane.getStyleClass().contains("dark-theme")) rootPane.getStyleClass().add("dark-theme");
        } else {
            switchBackground.setFill(Color.web("#bdc3c7"));
            rootPane.getStyleClass().remove("dark-theme");
        }

        double targetX = SessionManager.isDarkMode ? 16 : -16;

        if (animate) {
            TranslateTransition transition = new TranslateTransition(Duration.millis(250), switchKnob);
            transition.setToX(targetX);
            transition.play();
        } else {
            switchKnob.setTranslateX(targetX);
        }
    }

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
                        btnNavProfile.setGraphic(createCircularAvatar(fullImageUrl, 18));
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

    // ==========================================
    // HÀM LOAD VIEW NÂNG CẤP (CÓ HIỆU ỨNG LOADING TOÀN CỤC)
    // ==========================================
    public void loadView(String fxmlPath) {
        // 1. TẠO MÀN HÌNH LOADING TẠM THỜI BẰNG CODE JAVA
        VBox loadingBox = new VBox(20);
        loadingBox.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.control.ProgressIndicator spinner = new javafx.scene.control.ProgressIndicator();
        spinner.setPrefSize(55, 55);
        spinner.setStyle("-fx-progress-color: #3b5998;");

        Label lblLoading = new Label("Đang xử lý, vui lòng chờ...");
        lblLoading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #7f8c8d;");

        loadingBox.getChildren().addAll(spinner, lblLoading);

        // 2. Gắn Loading vào khu vực nội dung ngay lập tức (xóa trắng trang cũ)
        contentArea.getChildren().setAll(loadingBox);

        // 3. Dùng PauseTransition nhường cho UI 50ms để nó kịp vẽ cái Vòng xoay quay tít lên màn hình
        PauseTransition pause = new PauseTransition(Duration.millis(50));
        pause.setOnFinished(e -> {
            try {
                // Reset chat listener
                com.auction.util.GlobalWebSocketManager.currentActiveChatPartner = null;
                com.auction.util.GlobalWebSocketManager.setActiveChatListener(null);

                // Giai đoạn này gây lag nhẹ (load FXML), nhưng vòng xoay đã được vẽ ra rồi nên người dùng không thấy đơ
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Node view = loader.load();

                // LƯU LẠI CONTROLLER CỦA MÀN HÌNH ĐANG MỞ
                currentActiveController = loader.getController();

                // Nạp xong giao diện mới thì thay thế vòng xoay Loading
                contentArea.getChildren().setAll(view);

            } catch (IOException ex) {
                ex.printStackTrace();
                Label lblError = new Label("Lỗi khi tải giao diện!");
                lblError.setTextFill(Color.RED);
                contentArea.getChildren().setAll(lblError);
            }
        });

        pause.play();
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

    @FXML
    public void toggleSidebar() {
        if (isSidebarExpanded) collapseSidebar();
        else expandSidebar();
    }

    private void expandSidebar() {
        if (!isSidebarExpanded) {
            isSidebarExpanded = true;
            iconToggle.setContent("M 18 6 L 6 18 M 6 6 L 18 18");
            animateSidebarWidth(280);
        }
    }

    private void collapseSidebar() {
        if (isSidebarExpanded) {
            isSidebarExpanded = false;
            iconToggle.setContent("M 3 12 h 18 M 3 6 h 18 M 3 18 h 18");
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

    // ==========================================
    // HỆ THỐNG QUÉT THÔNG BÁO REAL-TIME
    // ==========================================
    private void startNotificationChecker() {
        if (SessionManager.userName == null) return;

        // Cứ 3 giây quét 1 lần
        notifCheckerTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            if (SessionManager.userName == null) { notifCheckerTimeline.stop(); return; }

            ApiService.getAsync("/notifications/" + SessionManager.userName).thenAccept(res -> {
                Platform.runLater(() -> {
                    if (res.statusCode() == 200) {
                        try {
                            com.auction.model.ApiResponse apiRes = ApiService.gson.fromJson(res.body(), com.auction.model.ApiResponse.class);
                            if (apiRes.code == 1000) {
                                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<NotificationModel>>(){}.getType();
                                List<NotificationModel> list = ApiService.gson.fromJson(apiRes.result, listType);

                                int newCount = (list != null) ? list.size() : 0;

                                // LẦN ĐẦU TIÊN KHỞI ĐỘNG (Set biến đếm, cập nhật UI, không nhảy Toast)
                                if (currentNotifCount == -1) {
                                    currentNotifCount = newCount;
                                    updateNotificationCount(newCount);
                                    return;
                                }

                                // CÓ THÔNG BÁO MỚI TINH (Số lượng tăng lên)
                                if (newCount > currentNotifCount && list != null && !list.isEmpty()) {
                                    // 1. Cập nhật số đếm ở Sidebar
                                    updateNotificationCount(newCount);

                                    // 2. Lấy cái thông báo mới nhất (đứng đầu danh sách)
                                    NotificationModel newestNotif = list.get(0);

                                    // 3. KIỂM TRA MÀN HÌNH HIỆN TẠI ĐỂ QUYẾT ĐỊNH XỬ LÝ
                                    if (currentActiveController instanceof NotificationController) {
                                        // Đang ở màn thông báo -> Không văng Toast -> Cập nhật list mượt mà
                                        ((NotificationController) currentActiveController).loadData();
                                    } else {
                                        // Đang ở màn khác -> Bắn Toast góc màn hình
                                        com.auction.util.ToastNotification.show(newestNotif.title, newestNotif.description);
                                    }
                                }

                                // Có thể có người xóa thông báo, nên ta luôn update lại current count
                                currentNotifCount = newCount;
                                updateNotificationCount(newCount);
                            }
                        } catch (Exception ex) {
                            System.out.println("Lỗi quét thông báo ngầm: " + ex.getMessage());
                        }
                    }
                });
            });
        }));
        notifCheckerTimeline.setCycleCount(Timeline.INDEFINITE);
        notifCheckerTimeline.play();
    }

    public void updateNotificationCount(int count) {
        Platform.runLater(() -> {
            if (lblNotifBadge != null) {
                if (count > 0) {
                    lblNotifBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                    lblNotifBadge.setVisible(true);
                } else {
                    lblNotifBadge.setVisible(false);
                }
            }
        });
    }

    // ==========================================
    // HÀM CẬP NHẬT SỐ LƯỢNG TIN NHẮN CHƯA ĐỌC
    // ==========================================
    public void updateChatBadgeCount() {
        Platform.runLater(() -> {
            if (lblChatBadge != null) {
                // Đếm xem có bao nhiêu người đang có chấm xanh
                int count = com.auction.util.GlobalWebSocketManager.globalUnreadUsers.size();

                if (count > 0) {
                    lblChatBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                    lblChatBadge.setVisible(true);
                    lblChatBadge.setManaged(true);
                } else {
                    lblChatBadge.setVisible(false);
                    lblChatBadge.setManaged(false);
                }
            }
        });
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
        if (notifCheckerTimeline != null) notifCheckerTimeline.stop();
        SessionManager.logout();

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Thông báo khẩn cấp");
        alert.setHeaderText("TÀI KHOẢN ĐÃ BỊ KHÓA!");
        alert.setContentText("Tài khoản của bạn vừa bị Admin khóa do vi phạm.\nBạn sẽ bị đăng xuất ngay lập tức.");
        alert.getDialogPane().setPrefSize(450, 250);
        alert.getDialogPane().setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        com.auction.util.AlertUtils.applyStyle(alert);
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

            // Nhớ Controller vào biến để checker biết
            currentActiveController = loader.getController();

            com.auction.controller.search.SearchController controller = loader.getController();
            controller.executeSearch(keyword);

            for (Button btn : allMenuButtons) if (btn != null) btn.getStyleClass().remove("active-menu-btn");

            contentArea.getChildren().setAll(view);
            collapseSidebar();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ==========================================
    // HÀM CHUYỂN NHANH ĐẾN ĐOẠN CHAT CỤ THỂ
    // ==========================================
    public void openSpecificChat(String targetUsername) {
        // 1. Gắn tên người cần mở vào biến tĩnh (static) của ChatController
        com.auction.controller.chat.ChatController.targetUsernameToOpen = targetUsername;

        // 2. Chuyển UI sang tab Chat (giống như khi bấm nút Chat ở Sidebar)
        showChat(null);
    }
}
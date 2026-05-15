package com.auction.controller.dashboard;

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
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class MainController {

    // --- SINGLETON ĐỂ CÁC MÀN HÌNH KHÁC GỌI ĐƯỢC MAIN CONTROLLER ---
    private static MainController instance;

    public MainController() {
        instance = this;
    }

    public static MainController getInstance() {
        return instance;
    }
    // ---------------------------------------------------------------

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
    @FXML private Button btnNavProfile;

    private List<Button> allMenuButtons;
    private Timeline banCheckerTimeline;

    @FXML private Button btnIconChat;
    @FXML private Button btnNavChat;

    @FXML private TextField txtSearch;

    @FXML
    public void initialize() {
        // Gom tất cả nút vào 1 danh sách (Nhớ bổ sung 2 nút Chat vào đây)
        allMenuButtons = Arrays.asList(
                btnIconHome, btnIconWallet, btnIconAuction, btnIconAdd, btnIconNotif, btnIconProfile, btnIconChat,
                btnNavHome, btnNavWallet, btnNavAuction, btnNavAdd, btnNavNotif, btnNavProfile, btnNavChat
        );

        loadUserInfo();
        showDashboard(null); // Mở trang chủ mặc định ban đầu
        startClock();
        startBanChecker();
    }

    /**
     * Tải thông tin người dùng (Tên & Avatar).
     * Để public để ProfileController có thể gọi lại hàm này khi đổi Avatar thành công.
     */
    public void loadUserInfo() {
        if (btnNavProfile == null || SessionManager.userName == null) return;

        // Set tạm tên trước khi có dữ liệu từ API
        btnNavProfile.setText("   " + SessionManager.userName);

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

                        // ========================================================
                        // CHỐNG CACHE: Bắt buộc JavaFX tải ảnh mới nhất từ Server
                        // Dùng ApiService.BASE_URL thay vì localhost gán cứng
                        // ========================================================
                        String fullImageUrl = ApiService.BASE_URL + avatarPath + "?t=" + System.currentTimeMillis();

                        // Tạo Avatar tròn cho chế độ thu gọn
                        ImageView smallAvatar = createCircularAvatar(fullImageUrl, 16);
                        btnIconProfile.setText("");
                        btnIconProfile.setGraphic(smallAvatar);

                        // Tạo Avatar tròn cho chế độ mở rộng
                        ImageView expandedAvatar = createCircularAvatar(fullImageUrl, 16);
                        String currentText = btnNavProfile.getText().replace("👤", "").trim();
                        btnNavProfile.setText("   " + currentText);
                        btnNavProfile.setGraphic(expandedAvatar);
                    }
                }
            });
        });
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
            System.err.println("Lỗi khi tải file FXML: " + fxmlPath);
        }
    }

    // Hàm tô màu nút được chọn
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
        loadView("/com/auction/view/dashboard/Dashboard.fxml");
        setActiveButton(btnIconHome, btnNavHome);
        closeSidebar();
    }

    @FXML public void showWallet(ActionEvent event) {
        loadView("/com/auction/view/wallet/Wallet.fxml");
        setActiveButton(btnIconWallet, btnNavWallet);
        closeSidebar();
    }

    @FXML public void showAuctionList(ActionEvent event) {
        loadView("/com/auction/view/auction/AuctionList.fxml");
        setActiveButton(btnIconAuction, btnNavAuction);
        closeSidebar();
    }

    @FXML public void handleShowMyProducts(ActionEvent event) {
        loadView("/com/auction/view/auction/MyAuctionList.fxml");
        setActiveButton(btnIconAdd, btnNavAdd);
        closeSidebar();
    }

    @FXML public void showNotification(ActionEvent event) {
        loadView("/com/auction/view/notification/NotificationList.fxml");
        setActiveButton(btnIconNotif, btnNavNotif);
        closeSidebar();
    }

    @FXML public void showProfile(ActionEvent event) {
        loadView("/com/auction/view/profile/Profile.fxml");
        setActiveButton(btnIconProfile, btnNavProfile);
        closeSidebar();
    }

    // THÊM HÀM CHUYỂN TRANG NÀY VÀO KHU VỰC CHUYỂN TRANG
    @FXML public void showChat(ActionEvent event) {
        // Tí nữa chúng ta sẽ tạo file Chat.fxml sau
        loadView("/com/auction/view/chat/Chat.fxml");
        setActiveButton(btnIconChat, btnNavChat);
        closeSidebar();
    }

    // ====== HÀM XỬ LÝ HIỆU ỨNG SIDEBAR ======

    @FXML
    public void openSidebar() {
        if (expandedSidebar != null && drawerOverlay != null) {
            drawerOverlay.setVisible(true);
            TranslateTransition slide = new TranslateTransition(Duration.millis(300), expandedSidebar);
            slide.setToX(280); // Trượt menu ra ngoài (280px)
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
            slide.setToX(0); // Trượt menu trở về vị trí cũ
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
        // Admin thì không bao giờ bị khóa, bỏ qua để tiết kiệm tài nguyên
        if (SessionManager.userName == null || "ADMIN".equals(SessionManager.role)) return;

        banCheckerTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            // Nếu người dùng đã tự đăng xuất, dừng quét luôn
            if (SessionManager.userName == null) {
                banCheckerTimeline.stop();
                return;
            }

            // Gọi API Profile để kiểm tra
            ApiService.getAsync("/users/profile/" + SessionManager.userName).thenAccept(res -> {
                Platform.runLater(() -> {
                    if (res.statusCode() == 200) {
                        com.auction.model.ApiResponse apiRes = ApiService.gson.fromJson(res.body(), com.auction.model.ApiResponse.class);
                        if (apiRes.code == 1000) {
                            com.auction.model.UserProfile profile = ApiService.gson.fromJson(apiRes.result, com.auction.model.UserProfile.class);

                            // NẾU PHÁT HIỆN BỊ BAN -> ĐÁ VĂNG NGAY LẬP TỨC
                            if (profile.banned) {
                                forceLogoutBannedUser();
                            }
                        }
                    }
                });
            });
        }));
        banCheckerTimeline.setCycleCount(Timeline.INDEFINITE);
        banCheckerTimeline.play();
    }

    /**
     * Tạo ImageView với khung hình tròn
     */
    private ImageView createCircularAvatar(String imageUrl, double radius) {
        Image image = new Image(imageUrl, radius * 2, radius * 2, true, true, true);

        // Bắt lỗi nếu link ảnh hỏng/server sập
        image.errorProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                System.err.println("Cảnh báo: Không thể tải ảnh từ URL -> " + imageUrl);
            }
        });

        ImageView imageView = new ImageView(image);
        Circle clip = new Circle(radius, radius, radius);
        imageView.setClip(clip);

        return imageView;
    }

    private void forceLogoutBannedUser() {
        // 1. Dừng bộ quét
        if (banCheckerTimeline != null) banCheckerTimeline.stop();

        // 2. Xóa session
        SessionManager.logout();

        // 3. Hiển thị thông báo cỡ lớn
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Thông báo khẩn cấp");
        alert.setHeaderText("TÀI KHOẢN ĐÃ BỊ KHÓA!");
        alert.setContentText("Tài khoản của bạn vừa bị Admin khóa do vi phạm.\nBạn sẽ bị đăng xuất ngay lập tức.");

        // Phóng to khung Dialog để user thấy rõ
        alert.getDialogPane().setPrefSize(450, 250);
        alert.getDialogPane().setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        alert.showAndWait();

        // 4. Chuyển về màn hình đăng nhập
        try {
            javafx.scene.Parent root = FXMLLoader.load(getClass().getResource("/com/auction/view/dashboard/Login.fxml"));
            javafx.stage.Stage stage = (javafx.stage.Stage) contentArea.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.centerOnScreen();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // BẮT SỰ KIỆN KHI NGƯỜI DÙNG ẤN ENTER TRONG Ô TÌM KIẾM
    @FXML
    public void handleSearch() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) return; // Không nhập gì thì không tìm

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/search/Search.fxml"));
            Node view = loader.load();

            // Ép kiểu controller và truyền từ khóa tìm kiếm sang
            com.auction.controller.search.SearchController controller = loader.getController();
            controller.executeSearch(keyword);

            // Bỏ active tất cả các nút ở Sidebar vì ta đang ở trang Search
            for (Button btn : allMenuButtons) {
                if (btn != null) btn.getStyleClass().remove("active-menu-btn");
            }

            contentArea.getChildren().setAll(view);
            closeSidebar();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
package com.auction.controller;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    @FXML private BorderPane mainPane;
    @FXML private VBox sideBar;
    @FXML private Button btnHome, btnWallet, btnAuction, btnAdd;
    @FXML private Label lblUserName;

    private boolean isSidebarExpanded = false;
    private final double SIDEBAR_MIN = 80.0;
    private final double SIDEBAR_MAX = 250.0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Mặc định khi mở app lên là hiển thị Trang chủ
        showHome();
    }

    // --- XỬ LÝ HIỆU ỨNG TRƯỢT SIDEBAR ---
    @FXML
    private void toggleSidebar() {
        Timeline timeline = new Timeline();
        if (isSidebarExpanded) {
            // Thu gọn lại
            KeyValue kv = new KeyValue(sideBar.prefWidthProperty(), SIDEBAR_MIN);
            KeyFrame kf = new KeyFrame(Duration.millis(200), kv);
            timeline.getKeyFrames().add(kf);
            timeline.setOnFinished(e -> updateMenuTexts(false));
        } else {
            // Mở rộng ra
            updateMenuTexts(true);
            KeyValue kv = new KeyValue(sideBar.prefWidthProperty(), SIDEBAR_MAX);
            KeyFrame kf = new KeyFrame(Duration.millis(200), kv);
            timeline.getKeyFrames().add(kf);
        }
        timeline.play();
        isSidebarExpanded = !isSidebarExpanded;
    }

    private void updateMenuTexts(boolean expanded) {
        if (expanded) {
            btnHome.setText(" 🏠   Trang chủ");
            btnWallet.setText(" 💼   Ví tiền");
            btnAuction.setText(" ⚖   Đấu giá");
            btnAdd.setText(" ⊕   Thêm sản phẩm");
            lblUserName.setText("Pipodaucatmoi");
        } else {
            btnHome.setText("🏠");
            btnWallet.setText("💼");
            btnAuction.setText("⚖");
            btnAdd.setText("⊕");
            lblUserName.setText("");
        }
    }

    // --- XỬ LÝ CHUYỂN TRANG ---
    @FXML
    private void showHome() {
        loadView("/com/auction/view/Dashboard.fxml");
        setActiveButton(btnHome);
    }

    @FXML
    private void showWallet() {
        loadView("/com/auction/view/Wallet.fxml");
        setActiveButton(btnWallet);
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            mainPane.setCenter(view); // Thay thế nội dung ở giữa
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Đổi màu style vạch trắng cho nút đang được chọn
    private void setActiveButton(Button activeBtn) {
        btnHome.getStyleClass().remove("nav-btn-active");
        btnWallet.getStyleClass().remove("nav-btn-active");
        btnAuction.getStyleClass().remove("nav-btn-active");
        btnAdd.getStyleClass().remove("nav-btn-active");

        activeBtn.getStyleClass().add("nav-btn-active");
    }
}
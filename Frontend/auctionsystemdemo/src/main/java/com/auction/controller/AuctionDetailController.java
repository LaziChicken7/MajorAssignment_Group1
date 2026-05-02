package com.auction.controller;

import com.auction.model.AuctionItem;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

public class AuctionDetailController {
    @FXML private Label lblId, lblName, lblTime, lblStartPrice, lblMyBid, lblCurrentPrice, lblConfirmAmount, lblBidStatus;
    @FXML private TextField txtBidAmount;
    @FXML private VBox paneConfirm;
    @FXML private HBox toastSuccess, toastError;
    @FXML private Button btnBidAction;

    private AuctionItem currentItem;

    public void setAuctionData(AuctionItem item) {
        this.currentItem = item;
        updateUI();

        // LOGIC: CHO ĐẤU GIÁ HAY KHÔNG
        boolean isFinished = "FINISHED".equals(item.getStatus()) || "SUCCESS".equals(item.getStatus());
        if (isFinished || item.getTimeLeft().equals("00:00:00")) {
            btnBidAction.setDisable(true);
            btnBidAction.setText("Đã kết thúc");
            txtBidAmount.setDisable(true);
            lblBidStatus.setText("Phiên đấu giá đã kết thúc.");
        }
    }

    private void updateUI() {
        if (currentItem == null) return;
        lblId.setText(currentItem.getId());
        lblName.setText(currentItem.getName());
        lblTime.setText(currentItem.getTimeLeft());
        lblCurrentPrice.setText(String.format("%,.0f VND", currentItem.getCurrentPrice()));
        lblStartPrice.setText(String.format("%,.0f VND", currentItem.getStartPrice()));
        lblMyBid.setText(String.format("%,.0f VND", currentItem.getMyBid()));
    }

    @FXML
    private void handleBidClick() {
        if (txtBidAmount.getText().isEmpty()) return;
        lblConfirmAmount.setText(txtBidAmount.getText() + " VND");
        paneConfirm.setVisible(true); // Hiện Popup xác nhận (Hình 3)
    }

    @FXML
    private void processConfirmBid() {
        try {
            double bidValue = Double.parseDouble(txtBidAmount.getText().replace(".", ""));
            paneConfirm.setVisible(false);

            if (bidValue <= currentItem.getCurrentPrice()) {
                showToast(toastError); // Thông báo ĐỎ (Hình 5)
            } else {
                currentItem.setCurrentPrice(bidValue);
                currentItem.setMyBid(bidValue);
                com.auction.controller.AuctionService.saveToFile(); // Lưu lại vào ổ cứng ngay
                updateUI();
                showToast(toastSuccess); // Thông báo XANH (Hình 4)
            }
        } catch (Exception e) { paneConfirm.setVisible(false); }
    }

    @FXML private void handleCancelPopup() { paneConfirm.setVisible(false); }

    private void showToast(HBox toast) {
        toast.setVisible(true);
        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(e -> toast.setVisible(false));
        delay.play();
    }

    @FXML
    private void goBack() {
        try {
            // Load lại Main để giữ Sidebar
            Node view = FXMLLoader.load(getClass().getResource("/com/auction/view/AuctionList.fxml"));
            StackPane contentArea = (StackPane) txtBidAmount.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
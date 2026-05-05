package com.auction.controller;

import com.auction.model.AuctionItem;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

public class AuctionDetailController {
    // Thêm txtDescription và productImage vào khai báo
    @FXML private Label lblId, lblName, lblTime, lblStartPrice, lblMyBid, lblCurrentPrice, lblConfirmAmount, lblBidStatus;
    @FXML private TextField txtBidAmount;
    @FXML private TextArea txtDescription; // Ô mô tả sản phẩm
    @FXML private ImageView productImage;   // Khung ảnh sản phẩm
    @FXML private VBox paneConfirm;
    @FXML private HBox toastSuccess, toastError;
    @FXML private Button btnBidAction;

    private AuctionItem currentItem;

    public void setAuctionData(AuctionItem item) {
        this.currentItem = item;
        updateUI();

        // LOGIC: KHÓA ĐẤU GIÁ NẾU ĐÃ KẾT THÚC
        boolean isFinished = "FINISHED".equals(item.getStatus()) || "SUCCESS".equals(item.getStatus());
        if (isFinished || "00:00:00".equals(item.getTimeLeft())) {
            btnBidAction.setDisable(true);
            btnBidAction.setText("Đã kết thúc");
            txtBidAmount.setDisable(true);
            if (lblBidStatus != null) lblBidStatus.setText("Phiên đấu giá đã kết thúc.");
        }
    }

    private void updateUI() {
        if (currentItem == null) return;

        // Đổ dữ liệu cơ bản
        lblId.setText(currentItem.getId());
        lblName.setText(currentItem.getName());
        lblTime.setText(currentItem.getTimeLeft());
        lblCurrentPrice.setText(String.format("%,.0f VND", currentItem.getCurrentPrice()));
        lblStartPrice.setText(String.format("%,.0f VND", currentItem.getStartPrice()));
        lblMyBid.setText(String.format("%,.0f VND", currentItem.getMyBid()));

        // 1. HIỂN THỊ MÔ TẢ (Lỗi của bạn nằm ở đây)
        if (txtDescription != null) {
            txtDescription.setText(currentItem.getDescription());
        }

        // 2. HIỂN THỊ ẢNH (Nếu bạn đã code phần lưu ảnh ở AddProduct)
        // Lưu ý: Nếu sp mới thêm từ form, bạn cần đảm bảo đường dẫn ảnh hợp lệ
        /*
        if (productImage != null && currentItem.getImagePath() != null) {
            try {
                productImage.setImage(new Image(currentItem.getImagePath()));
            } catch (Exception e) {
                System.out.println("Không load được ảnh sản phẩm.");
            }
        }
        */
    }

    @FXML
    private void handleBidClick() {
        if (txtBidAmount.getText().isEmpty()) return;
        lblConfirmAmount.setText(txtBidAmount.getText() + " VND");
        paneConfirm.setVisible(true);
    }

    @FXML
    private void processConfirmBid() {
        try {
            // Xử lý giá tiền (Xóa dấu chấm nếu có)
            double bidValue = Double.parseDouble(txtBidAmount.getText().replace(".", "").replace(",", ""));
            paneConfirm.setVisible(false);

            if (bidValue <= currentItem.getCurrentPrice()) {
                showToast(toastError);
            } else {
                currentItem.setCurrentPrice(bidValue);
                currentItem.setMyBid(bidValue);
                // Lưu lại vào file .dat ngay lập tức
                com.auction.controller.AuctionService.saveToFile();
                updateUI();
                com.auction.controller.NotificationService.addNotification("Bạn vừa đặt giá " + txtBidAmount.getText() + " VND cho " + currentItem.getName(), "SUCCESS");
                showToast(toastSuccess);
            }
        } catch (Exception e) {
            paneConfirm.setVisible(false);
            System.out.println("Lỗi định dạng giá tiền.");
        }
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
            Node view = FXMLLoader.load(getClass().getResource("/com/auction/view/AuctionList.fxml"));
            StackPane contentArea = (StackPane) txtBidAmount.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
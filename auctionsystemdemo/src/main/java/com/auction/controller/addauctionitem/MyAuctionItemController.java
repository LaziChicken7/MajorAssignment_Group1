package com.auction.controller.addauctionitem;

import com.auction.model.AuctionModel;
import com.auction.util.ApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class MyAuctionItemController {

    @FXML private ImageView imgThumbnail;
    @FXML private Label lblId;
    @FXML private Label lblName;
    @FXML private Label lblPrice;
    @FXML private Label lblTime;
    @FXML private Label lblStatus;
    @FXML private Button btnStartAuction;

    private AuctionModel currentItem;

    public void setData(AuctionModel item) {
        this.currentItem = item;
        if (item == null || item.bidProduct == null) return;

        // 1. Dữ liệu cơ bản
        lblId.setText(item.bidProduct.id.substring(0, 8).toUpperCase());
        lblName.setText(item.bidProduct.name);
        lblPrice.setText(String.format("%,.0f VND", item.highestBid).replace(",", "."));

        // 2. Logic Ảnh đại diện
        if (item.bidProduct.imageUrls != null && !item.bidProduct.imageUrls.isEmpty()) {
            String fullUrl = ApiService.BASE_URL + item.bidProduct.imageUrls.get(0);
            imgThumbnail.setImage(new Image(fullUrl, true)); // true: Load ngầm không giật lag
        } else {
            imgThumbnail.setImage(new Image("https://via.placeholder.com/120?text=No+Image", true));
        }

        // 3. Logic Thời gian, Trạng thái & Nút bắt đầu
        // Mặc định ẩn nút
        btnStartAuction.setVisible(false);
        btnStartAuction.setManaged(false);

        switch (item.status) {
            case "OPEN":
                lblStatus.setText("SẮP MỞ");
                lblStatus.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold; -fx-font-size: 16px;");
                lblTime.setText(item.startTime != null ? item.startTime.replace("T", " ") : "Chưa rõ");

                // NẾU LÀ SELLER VÀ STATUS LÀ OPEN -> HIỆN NÚT BẮT ĐẦU
                btnStartAuction.setVisible(true);
                btnStartAuction.setManaged(true);
                break;

            case "RUNNING":
                lblStatus.setText("ĐANG ĐẤU GIÁ");
                lblStatus.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold; -fx-font-size: 16px;");
                lblTime.setText(item.endTime != null ? item.endTime.replace("T", " ") : "Chưa rõ");
                break;

            case "FINISHED":
            case "PAID":
                lblStatus.setText("ĐÃ KẾT THÚC");
                lblStatus.setStyle("-fx-text-fill: #95a5a6; -fx-font-weight: bold; -fx-font-size: 16px;");
                lblTime.setText(item.endTime != null ? item.endTime.replace("T", " ") : "Chưa rõ");
                break;

            case "CANCELLED":
                lblStatus.setText("ĐÃ HỦY");
                lblStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 16px;");
                lblTime.setText("--:--");
                break;

            default:
                lblStatus.setText(item.status);
                lblTime.setText("--:--");
                break;
        }
    }

    @FXML
    private void handleStartAuction() {
        if (currentItem == null) return;

        System.out.println("Đang gọi API bắt đầu phiên: /auctions/" + currentItem.id + "/start");

        ApiService.putAsync("/auctions/" + currentItem.id + "/start", null).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    // Nếu Backend có bọc dữ liệu trong class ApiResponse
                    try {
                        com.auction.model.ApiResponse apiRes = ApiService.gson.fromJson(res.body(), com.auction.model.ApiResponse.class);
                        if (apiRes.code == 1000) {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Thành công"); alert.setHeaderText(null);
                            alert.setContentText("Đã bắt đầu phiên đấu giá!");
                            alert.showAndWait();

                            btnStartAuction.setVisible(false); btnStartAuction.setManaged(false);
                            lblStatus.setText("ĐANG ĐẤU GIÁ");
                            lblStatus.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold; -fx-font-size: 16px;");
                        } else {
                            // Trả về 200 nhưng code lỗi (VD: 1005 - Chưa đến giờ)
                            showErrorAlert(apiRes.message);
                        }
                    } catch (Exception e) {
                        // Trường hợp Backend trả về String trực tiếp không bọc ApiResponse
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Thành công"); alert.setHeaderText(null);
                        alert.setContentText("Đã bắt đầu phiên đấu giá!");
                        alert.showAndWait();
                    }
                } else {
                    // Backend văng lỗi 400, 404, 500...
                    System.out.println("Chi tiết lỗi từ Server: " + res.body());
                    try {
                        com.auction.model.ApiResponse errRes = ApiService.gson.fromJson(res.body(), com.auction.model.ApiResponse.class);
                        showErrorAlert("Lỗi từ Server: " + errRes.message);
                    } catch (Exception e) {
                        showErrorAlert("Mã lỗi HTTP: " + res.statusCode() + ". Vui lòng xem log ở Console IntelliJ/Eclipse.");
                    }
                }
            });
        });
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
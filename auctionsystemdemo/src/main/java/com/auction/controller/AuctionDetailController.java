package com.auction.controller;

import com.auction.model.ApiResponse;
import com.auction.model.AuctionModel;
import com.auction.model.PlaceBidRequest;
import com.auction.model.WalletDataResponse;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class AuctionDetailController {

    @FXML private Label lblId, lblName, lblTime, lblStartPrice, lblCurrentPrice, lblConfirmAmount, lblBalance;
    @FXML private Label lblMyBid;
    @FXML private TextField txtBidAmount;
    @FXML private TextArea txtDescription;
    @FXML private VBox paneConfirm;
    @FXML private HBox toastSuccess, toastError;
    @FXML private Label lblToastErrorMsg;
    @FXML private Button btnBidAction;
    @FXML private Label lblTimeTitle; // Thêm nhãn này


    private AuctionModel currentItem;
    private Timeline timeline;

    //Biến cho bước nhảy tiền
    private long currentBidValue = 0;
    private final long STEP_VALUE = 1000;
    private long currentHighestBid = 0;

    @FXML
    public void initialize() {
        if (txtBidAmount != null) {
            txtBidAmount.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null || newValue.isEmpty()) return;
                try {
                    String plainNumber = newValue.replace(".", "").replace(",", "");
                    if (!plainNumber.isEmpty()) {
                        currentBidValue = Long.parseLong(plainNumber);
                    }
                } catch (NumberFormatException e) {
                    txtBidAmount.setText(oldValue); // Nếu nhập chữ chặn lại, trả về số cũ
                }
            });
        }
    }

    public void setAuctionData(AuctionModel item) {
        this.currentItem = item;
        updateUI();
        loadBalance();

        initBidData();

        if ("RUNNING".equals(item.status)) {
            btnBidAction.setDisable(false);
            btnBidAction.setText("Đấu giá");
            txtBidAmount.setDisable(false);
        } else if ("OPEN".equals(item.status)) {
            btnBidAction.setDisable(true);
            btnBidAction.setText("Chưa đến phiên");
            txtBidAmount.setDisable(true);
        } else {
            btnBidAction.setDisable(true);
            btnBidAction.setText("Đã kết thúc");
            txtBidAmount.setDisable(true);
        }
    }

    private void loadBalance() {
        if (SessionManager.userName == null) return;
        ApiService.getAsync("/payments/" + SessionManager.userName + "/history").thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        WalletDataResponse wallet = ApiService.gson.fromJson(apiRes.result, WalletDataResponse.class);
                        if (lblBalance != null) {
                            lblBalance.setText(String.format("%,.0f VND", wallet.moneyOnWallet).replace(",", "."));
                        }
                    }
                }
            });
        });
    }

    private void updateUI() {
        if (currentItem == null) return;

        lblId.setText("SP: " + currentItem.bidProduct.id.substring(0, 4).toUpperCase());
        lblName.setText(currentItem.bidProduct.name);
        lblStartPrice.setText(String.format("%,.0f VND", currentItem.bidProduct.startPrice).replace(",", "."));
        lblCurrentPrice.setText(String.format("%,.0f VND", currentItem.highestBid).replace(",", "."));

        double myHighestBid = currentItem.getMyHighestBid(SessionManager.userName);
        if (lblMyBid != null) lblMyBid.setText(String.format("%,.0f VND", myHighestBid).replace(",", "."));

        String baseColor;
        switch (currentItem.status) {
            case "RUNNING": baseColor = "#f39c12"; break;
            case "OPEN": baseColor = "#3498db"; break;
            case "FINISHED":
            case "PAID": baseColor = "#2ecc71"; break;
            case "CANCELLED": baseColor = "#e74c3c"; break;
            default: baseColor = "#95a5a6"; break;
        }

        if (lblTimeTitle != null) {
            if ("OPEN".equals(currentItem.status)) lblTimeTitle.setText("Sắp bắt đầu sau:");
            else if ("RUNNING".equals(currentItem.status)) lblTimeTitle.setText("Thời gian còn lại:");
            else lblTimeTitle.setText("Thời gian:");
        }

        if (timeline != null) timeline.stop();

        if (("RUNNING".equals(currentItem.status) && currentItem.endTime != null) || ("OPEN".equals(currentItem.status) && currentItem.startTime != null)) {
            try {
                String targetTimeStr = "OPEN".equals(currentItem.status) ? currentItem.startTime : currentItem.endTime;
                targetTimeStr = targetTimeStr.contains("T") ? targetTimeStr : targetTimeStr.replace(" ", "T");
                LocalDateTime targetTime = LocalDateTime.parse(targetTimeStr);

                timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                    LocalDateTime now = LocalDateTime.now();

                    if (now.isAfter(targetTime)) {
                        lblTime.setText("00:00:00");
                        lblTime.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 16px;");
                        timeline.stop();
                        btnBidAction.setDisable(true);
                        btnBidAction.setText("Đã kết thúc");
                        txtBidAmount.setDisable(true);
                    } else {
                        java.time.Duration duration = java.time.Duration.between(now, targetTime);
                        long hours = duration.toHours();
                        long minutes = duration.toMinutesPart();
                        long seconds = duration.toSecondsPart();

                        lblTime.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));

                        if ("RUNNING".equals(currentItem.status) && hours == 0 && minutes < 10) {
                            lblTime.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 16px;");
                        } else {
                            lblTime.setStyle("-fx-background-color: " + baseColor + "; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 16px;");
                        }
                    }
                }));
                timeline.setCycleCount(Timeline.INDEFINITE);
                timeline.play();
            } catch (Exception ex) {
                lblTime.setText("Lỗi giờ");
            }
        } else {
            lblTime.setText("00:00:00");
            lblTime.setStyle("-fx-background-color: " + baseColor + "; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 16px;");
        }

        if (txtDescription != null) txtDescription.setText(currentItem.bidProduct.description);
    }

    @FXML
    private void handleBidClick() {
        String amountStr = txtBidAmount.getText().replace(".", "").replace(",", "").trim();
        if (amountStr.isEmpty()) return;
        try {
            double bidValue = Double.parseDouble(amountStr);
            if (bidValue <= currentItem.highestBid) {
                showToastError("Giá phải lớn hơn giá hiện tại!");
                return;
            }
            lblConfirmAmount.setText(String.format("%,.0f VND", bidValue).replace(",", "."));
            paneConfirm.setVisible(true);
        } catch (Exception e) {
            showToastError("Số tiền không hợp lệ!");
        }
    }

    @FXML
    private void processConfirmBid() {
        paneConfirm.setVisible(false);
        double bidValue = Double.parseDouble(txtBidAmount.getText().replace(".", "").replace(",", "").trim());

        PlaceBidRequest request = new PlaceBidRequest(SessionManager.userName, bidValue);

        ApiService.postAsync("/auctions/" + currentItem.id + "/place-bid", request).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() >= 200 && res.statusCode() < 300) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        currentItem.highestBid = bidValue;
                        if (currentItem.bidTransactions == null) currentItem.bidTransactions = new ArrayList<>();
                        AuctionModel.BidTransactionModel newTx = new AuctionModel.BidTransactionModel();
                        newTx.bidAmount = bidValue;
                        newTx.bidder = new AuctionModel.BidderModel();
                        newTx.bidder.userName = SessionManager.userName;
                        currentItem.bidTransactions.add(newTx);

                        updateUI();
                        loadBalance();
                        showToastSuccess();

                        initBidData();
                    } else {
                        showToastError(apiRes.message);
                    }
                } else {
                    try {
                        ApiResponse errRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        showToastError(errRes.message);
                    } catch (Exception e) {
                        showToastError("Lỗi hệ thống: " + res.statusCode());
                    }
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> showToastError("Mất kết nối máy chủ!"));
            return null;
        });
    }

    @FXML private void handleCancelPopup() { paneConfirm.setVisible(false); }

    private void showToastSuccess() {
        toastSuccess.setVisible(true);
        PauseTransition delay = new PauseTransition(Duration.seconds(2.5));
        delay.setOnFinished(e -> toastSuccess.setVisible(false));
        delay.play();
    }

    private void showToastError(String msg) {
        if(lblToastErrorMsg != null) lblToastErrorMsg.setText(msg);
        toastError.setVisible(true);
        PauseTransition delay = new PauseTransition(Duration.seconds(2.5));
        delay.setOnFinished(e -> toastError.setVisible(false));
        delay.play();
    }

    @FXML
    private void goBack() {
        if (timeline != null) timeline.stop();
        try {
            Node view = FXMLLoader.load(getClass().getResource("/com/auction/view/AuctionList.fxml"));
            StackPane contentArea = (StackPane) txtBidAmount.getScene().lookup("#contentArea");
            if(contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- CÁC HÀM XỬ LÝ NÚT TĂNG GIẢM ---
    public void initBidData() {
        if (currentItem != null) {
            this.currentHighestBid = (long) currentItem.highestBid;
            // Giá mới = Giá hiện tại + 1000
            this.currentBidValue = this.currentHighestBid;
            updateBidTextField();
        }
    }

    @FXML
    public void increaseBid() {
        currentBidValue += STEP_VALUE;
        updateBidTextField();
    }

    @FXML
    public void decreaseBid() {
        // Chỉ cho trừ nếu tiền vẫn lớn hơn mức giá cho phép (giá cao nhất + 1000)
        if (currentBidValue > currentHighestBid + STEP_VALUE) {
            currentBidValue -= STEP_VALUE;
            updateBidTextField();
        }
    }

    private void updateBidTextField() {
        if (txtBidAmount != null) {
            txtBidAmount.setText(String.format("%,d", currentBidValue).replace(",", "."));
        }
    }
}
package com.auction.controller.auction;

import com.auction.controller.profile.SellerProfileController;
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
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.chart.LineChart;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.paint.Color;

public class AuctionDetailController {

    @FXML private Label lblId, lblName, lblTime, lblStartPrice, lblCurrentPrice, lblConfirmAmount, lblBalance;
    @FXML private Label lblMyBid, lblHighestBidder;

    @FXML private TextField txtBidAmount;
    @FXML private TextArea txtDescription;
    @FXML private VBox paneConfirm;
    @FXML private HBox toastSuccess, toastError;
    @FXML private Label lblToastErrorMsg;

    @FXML private Button btnBidAction, btnAutoBidAction;

    @FXML private Label lblTimeTitle;
    @FXML private VBox vboxItemDetails;

    // --- ẢNH & BIỂU ĐỒ ---
    @FXML private LineChart<String, Number> priceChart;
    @FXML private ImageView productImageView;
    @FXML private Label btnPrevImage, btnNextImage, lblImageIndex;
    private Timeline chartTimeline;
    private List<String> imageUrls = new ArrayList<>();
    private int currentImageIndex = 0;

    // --- BƯỚC NHẢY TIỀN (STEPPER) TỰ ĐỘNG ---
    private long currentBidValue = 0;
    private long currentStepValue = 10000;
    private long currentHighestBid = 0;

    private AuctionModel currentItem;
    private Timeline timeline;

    // --- LỊCH SỬ & PROGRESS BAR ---
    @FXML private Label lblHistoryName, lblHistoryStartPrice, lblHistoryCurrentPrice, lblHistoryPercent;
    @FXML private ProgressBar bidProgressBar;
    @FXML private VBox vboxBidHistory;

    // --- PHÓNG TO ẢNH ---
    @FXML private VBox paneImageZoom;
    @FXML private ImageView zoomedImageView;

    // --- AUTO-BID BOT ---
    @FXML private VBox paneAutoBidConfig;
    @FXML private TextField txtAutoBidMaxAmount;
    @FXML private Label lblAutoBidStep;

    @FXML private ImageView imgSellerAvatar;
    @FXML private Label lblSellerName, lblSellerRating;
    @FXML private Label lblCurrentBotStatus;
    @FXML private Button btnSubmitAutoBid;
    @FXML private Label lblCurrentMaxBid;


    /* Ẩn/hiện số dư*/
    @FXML private Label eyeIconText;
    private String realBalanceTextDetail = "0 VND";
    private boolean isBalanceHiddenDetail = true;

    @FXML
    public void initialize() {
        // Áp dụng định dạng tiền tệ (vừa gõ vừa có dấu chấm) cho cả 2 ô nhập
        if (txtBidAmount != null) applyCurrencyFormat(txtBidAmount, value -> currentBidValue = value);
        if (txtAutoBidMaxAmount != null) applyCurrencyFormat(txtAutoBidMaxAmount, null);
    }

    /**
     * Hàm ma thuật: Vừa gõ vừa tự động định dạng 1.000.000 (Ngăn cách hàng nghìn)
     * Giữ nguyên vị trí con trỏ chuột không bị nhảy lung tung
     */
    private void applyCurrencyFormat(TextField textField, java.util.function.Consumer<Long> variableSetter) {
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                if (variableSetter != null) variableSetter.accept(0L);
                return;
            }
            // Chỉ giữ lại số
            String cleanString = newVal.replaceAll("[^\\d]", "");
            if (cleanString.isEmpty()) {
                textField.setText("");
                if (variableSetter != null) variableSetter.accept(0L);
                return;
            }

            try {
                long parsed = Long.parseLong(cleanString);
                if (variableSetter != null) variableSetter.accept(parsed);

                // Format kiểu VN: 1.000.000
                String formatted = String.format("%,d", parsed).replace(",", ".");

                if (!formatted.equals(newVal)) {
                    Platform.runLater(() -> {
                        int caret = textField.getCaretPosition();
                        int diff = formatted.length() - newVal.length();
                        textField.setText(formatted);
                        textField.positionCaret(Math.max(caret + diff, 0));
                    });
                }
            } catch (NumberFormatException e) {
                textField.setText(oldVal);
            }
        });
    }

    // ==============================================
    // THUẬT TOÁN TÍNH BƯỚC GIÁ ĐỘNG (1-2-5 RULE)
    // ==============================================
    private long calculateDynamicStep(double startPrice) {
        if (startPrice <= 0) return 10000;
        double rawStep = startPrice * 0.02; // 2% giá gốc
        if (rawStep < 10000) return 10000;

        double magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));
        double normalized = rawStep / magnitude;

        double niceDigit;
        if (normalized < 1.5) niceDigit = 1.0;
        else if (normalized < 3.5) niceDigit = 2.0;
        else if (normalized < 7.5) niceDigit = 5.0;
        else niceDigit = 10.0;

        return (long) (niceDigit * magnitude);
    }

    public void setAuctionData(AuctionModel item) {
        this.currentItem = item;
        updateUI();
        loadBalance();
        initBidHistory(item.id);
        initBidData();

        if ("RUNNING".equals(item.status)) {
            btnBidAction.setDisable(false);
            btnBidAction.setText("Thủ công");
            txtBidAmount.setDisable(false);
            if (btnAutoBidAction != null) btnAutoBidAction.setDisable(false);
        } else if ("OPEN".equals(item.status)) {
            btnBidAction.setDisable(true);
            btnBidAction.setText("Chưa đến phiên");
            txtBidAmount.setDisable(true);
            if (btnAutoBidAction != null) btnAutoBidAction.setDisable(true);
        } else {
            btnBidAction.setDisable(true);
            btnBidAction.setText("Đã kết thúc");
            txtBidAmount.setDisable(true);
            if (btnAutoBidAction != null) btnAutoBidAction.setDisable(true);
        }

        boolean isSeller = SessionManager.userName != null && item.seller != null && SessionManager.userName.equals(item.seller.userName);
        if (isSeller) {
            btnBidAction.setStyle(btnBidAction.getStyle() + "-fx-opacity: 0.5; -fx-cursor: default;");
            if (btnAutoBidAction != null) btnAutoBidAction.setStyle(btnAutoBidAction.getStyle() + "-fx-opacity: 0.5; -fx-cursor: default;");
            Tooltip sellerTooltip = new Tooltip("Bạn không thể đấu giá sản phẩm của chính mình!");
            sellerTooltip.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-size: 13.5px; -fx-font-weight: bold; -fx-padding: 8 12; -fx-background-radius: 6; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 3);");
            sellerTooltip.setShowDelay(Duration.millis(100));
            Tooltip.install(btnBidAction, sellerTooltip);
            if (btnAutoBidAction != null) Tooltip.install(btnAutoBidAction, sellerTooltip);
        }
    }

    public void initBidData() {
        if (currentItem != null) {
            this.currentStepValue = calculateDynamicStep(currentItem.bidProduct.startPrice);
            this.currentHighestBid = (long) currentItem.highestBid;

            // LÀM TRÒN: Tính giá trị hợp lệ thấp nhất tiếp theo
            long remainder = this.currentHighestBid % currentStepValue;
            long nextValidBid = this.currentHighestBid - remainder + currentStepValue; // Ép lên số tròn luôn

            this.currentBidValue = nextValidBid;
            updateBidTextField();
        }
    }

    // ==============================================
    // LOGIC NÚT + / - CHO ĐẤU GIÁ THỦ CÔNG
    // ==============================================
    @FXML
    public void increaseBid() {
        long remainder = currentBidValue % currentStepValue;
        if (remainder != 0) {
            currentBidValue = currentBidValue - remainder + currentStepValue; // Đang số lẻ -> Về số chẵn
        } else {
            currentBidValue += currentStepValue;
        }
        updateBidTextField();
    }

    @FXML
    public void decreaseBid() {
        long highestRemainder = currentHighestBid % currentStepValue;
        long minValidBid = currentHighestBid - highestRemainder + currentStepValue;

        long remainder = currentBidValue % currentStepValue;
        if (remainder != 0) {
            currentBidValue = currentBidValue - remainder; // Đang số lẻ -> Về số chẵn phía dưới
        } else {
            currentBidValue -= currentStepValue;
        }

        // Chặn không cho rớt qua giá tối thiểu
        if (currentBidValue < minValidBid) {
            currentBidValue = minValidBid;
        }
        updateBidTextField();
    }

    private void updateBidTextField() {
        if (txtBidAmount != null) txtBidAmount.setText(String.format("%,d", currentBidValue).replace(",", "."));
    }

    // ==============================================
    // TÍNH NĂNG CÀI ĐẶT AUTOBID (BOT)
    // ==============================================
    @FXML
    private void handleOpenAutoBid() {
        if (SessionManager.userName != null && currentItem.seller != null && SessionManager.userName.equals(currentItem.seller.userName)) return;

        // 1. Setup giao diện mặc định (Giả định user chưa cài bot bao giờ)
        if (txtAutoBidMaxAmount != null && lblAutoBidStep != null) {
            long highestRemainder = currentHighestBid % currentStepValue;
            long nextValidBid = currentHighestBid - highestRemainder + currentStepValue;
            long suggestMax = nextValidBid + (currentStepValue * 5);

            lblAutoBidStep.setText("Bước giá hệ thống: " + String.format("%,d", currentStepValue).replace(",", ".") + " VND");
            txtAutoBidMaxAmount.setText(String.format("%,d", suggestMax).replace(",", "."));

            lblCurrentBotStatus.setText("Trạng thái: Chưa cài đặt");
            lblCurrentBotStatus.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #95a5a6;"); // Xám

            // Ẩn cái Label hiển thị tiền đi vì chưa cài Bot
            if (lblCurrentMaxBid != null) {
                lblCurrentMaxBid.setVisible(false);
                lblCurrentMaxBid.setManaged(false);
            }
            if (btnSubmitAutoBid != null) btnSubmitAutoBid.setText("Kích hoạt Bot");
        }

        paneAutoBidConfig.setVisible(true);

        // 2. GỌI API KIỂM TRA BOT CỦA MÌNH
        ApiService.getAsync("/auctions/" + currentItem.id + "/my-autobid?username=" + SessionManager.userName).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    // Nếu API trả về 1000 và có số tiền (Nghĩa là Bot đang chạy)
                    if (apiRes.code == 1000 && apiRes.result != null) {
                        try {
                            long currentMax = apiRes.result.getAsLong();

                            // Đổi trạng thái sang Xanh lá
                            lblCurrentBotStatus.setText("Trạng thái: Đang hoạt động");
                            lblCurrentBotStatus.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2ecc71;");

                            // HIỂN THỊ MỨC TIỀN ĐANG CÀI ĐẶT BẰNG MÀU XANH DƯƠNG RÕ RÀNG
                            if (lblCurrentMaxBid != null) {
                                lblCurrentMaxBid.setText("Mức trần đang cài: " + String.format("%,d", currentMax).replace(",", ".") + " VND");
                                lblCurrentMaxBid.setVisible(true);
                                lblCurrentMaxBid.setManaged(true);
                            }

                            // Vẫn fill số tiền đó vào ô nhập để user dễ dàng cộng trừ (Chỉnh sửa)
                            txtAutoBidMaxAmount.setText(String.format("%,d", currentMax).replace(",", "."));

                            if (btnSubmitAutoBid != null) btnSubmitAutoBid.setText("Cập nhật Bot");
                        } catch (Exception e) {}
                    }
                }
            });
        });
    }

    @FXML
    public void increaseAutoBid() {
        try {
            long current = Long.parseLong(txtAutoBidMaxAmount.getText().replaceAll("[^\\d]", ""));
            long remainder = current % currentStepValue;
            if (remainder != 0) {
                current = current - remainder + currentStepValue; // Làm tròn lên
            } else {
                current += currentStepValue;
            }
            txtAutoBidMaxAmount.setText(String.format("%,d", current).replace(",", "."));
        } catch (Exception e) {}
    }

    @FXML
    public void decreaseAutoBid() {
        try {
            long current = Long.parseLong(txtAutoBidMaxAmount.getText().replaceAll("[^\\d]", ""));
            long remainder = current % currentStepValue;
            if (remainder != 0) {
                current = current - remainder; // Làm tròn xuống
            } else {
                current -= currentStepValue;
            }

            // Tính giá trị thấp nhất hợp lệ
            long highestRemainder = currentHighestBid % currentStepValue;
            long minValidBid = currentHighestBid - highestRemainder + currentStepValue;

            if (current < minValidBid) {
                current = minValidBid; // Không cho tụt quá mức
            }
            txtAutoBidMaxAmount.setText(String.format("%,d", current).replace(",", "."));
        } catch (Exception e) {}
    }

    @FXML
    private void processSetupAutoBid() {
        String amountStr = txtAutoBidMaxAmount.getText().replace(".", "").replace(",", "").trim();
        if (amountStr.isEmpty()) return;

        try {
            double maxAmount = Double.parseDouble(amountStr);
            if (maxAmount <= currentItem.highestBid + currentStepValue) {
                showToastError("Giá trần phải lớn hơn giá hiện tại!");
                return;
            }

            String url = "/auctions/" + currentItem.id + "/setup-autobid"
                    + "?username=" + SessionManager.userName
                    + "&maxAmount=" + maxAmount;

            ApiService.postAsync(url, null).thenAccept(res -> {
                Platform.runLater(() -> {
                    if (res.statusCode() >= 200 && res.statusCode() < 300) {
                        paneAutoBidConfig.setVisible(false); // Thành công thì mới đóng Popup
                        ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        if (apiRes.code == 1000) {
                            showToastSuccess();
                            initBidHistory(currentItem.id);
                        } else {
                            showToastError(apiRes.message);
                        }
                    } else {
                        // THẤT BẠI: KHÔNG ĐÓNG POPUP ĐỂ USER SỬA SỐ, ĐỌC LỖI TỪ BACKEND
                        try {
                            // Spring Boot mặc định khi văng RuntimeException sẽ trả về chuỗi JSON chứa biến "message"
                            // Gson sẽ tự động hứng và nhét vào biến errRes.message
                            ApiResponse errRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);

                            if (errRes != null && errRes.message != null && !errRes.message.isEmpty()) {
                                showToastError(errRes.message); // Hiện câu lỗi "Không phải là bội số...", "Phải cao hơn giá hiện tại..."
                            } else {
                                showToastError("Lỗi kích hoạt Bot: " + res.statusCode());
                            }
                        } catch (Exception e) {
                            showToastError("Lỗi hệ thống: " + res.statusCode());
                        }
                    }
                });
            }).exceptionally(ex -> {
                Platform.runLater(() -> showToastError("Mất kết nối hệ thống!"));
                return null;
            });
        } catch (Exception e) {
            showToastError("Dữ liệu nhập không hợp lệ!");
        }
    }

    @FXML private void handleCloseAutoBid() { paneAutoBidConfig.setVisible(false); }

    // ==============================================
    // CÁC HÀM CŨ GIỮ NGUYÊN (LOAD DATA, XÁC NHẬN BID, MỞ LỊCH SỬ...)
    // ==============================================
    private void loadBalance() {
        if (SessionManager.userName == null) return;
        ApiService.getAsync("/payments/" + SessionManager.userName + "/history").thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        WalletDataResponse wallet = ApiService.gson.fromJson(apiRes.result, WalletDataResponse.class);
                        if (lblBalance != null) {
                            realBalanceTextDetail = String.format("%,.0f VND", wallet.moneyOnWallet).replace(",", ".");
                            if (!isBalanceHiddenDetail) {
                                lblBalance.setText(realBalanceTextDetail);
                            } else {
                                lblBalance.setText("****** VND");
                            }
                        }

                    }
                }
            });
        });
    }

    private String getHighestBidderName(List<AuctionModel.BidTransactionModel> txs) {
        if (txs == null || txs.isEmpty()) return "Chưa có";
        AuctionModel.BidTransactionModel highestTx = txs.get(0);
        for (AuctionModel.BidTransactionModel tx : txs) if (tx.bidAmount > highestTx.bidAmount) highestTx = tx;
        return (highestTx.bidder != null && highestTx.bidder.userName != null) ? highestTx.bidder.userName : "Ẩn danh";
    }

    private void updateUI() {
        if (currentItem == null) return;
        lblId.setText("SP: " + currentItem.bidProduct.id.substring(0, 4).toUpperCase());
        lblName.setText(currentItem.bidProduct.name);
        lblStartPrice.setText(String.format("%,.0f VND", currentItem.bidProduct.startPrice).replace(",", "."));
        lblCurrentPrice.setText(String.format("%,.0f VND", currentItem.highestBid).replace(",", "."));

        if (lblHighestBidder != null) lblHighestBidder.setText(getHighestBidderName(currentItem.bidTransactions));

        double myHighestBid = currentItem.getMyHighestBid(SessionManager.userName);
        if (lblMyBid != null) lblMyBid.setText(String.format("%,.0f VND", myHighestBid).replace(",", "."));

        String baseColor = "#95a5a6";
        if ("RUNNING".equals(currentItem.status)) baseColor = "#f39c12";
        else if ("OPEN".equals(currentItem.status)) baseColor = "#3498db";
        else if ("FINISHED".equals(currentItem.status) || "PAID".equals(currentItem.status)) baseColor = "#2ecc71";
        else if ("CANCELLED".equals(currentItem.status)) baseColor = "#e74c3c";

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

                String finalBaseColor = baseColor;
                timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                    LocalDateTime now = LocalDateTime.now();
                    if (now.isAfter(targetTime)) {
                        lblTime.setText("00:00:00");
                        lblTime.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 16px;");
                        timeline.stop();
                        btnBidAction.setDisable(true);
                        btnBidAction.setText("Đã kết thúc");
                        txtBidAmount.setDisable(true);
                        if (btnAutoBidAction != null) btnAutoBidAction.setDisable(true);
                    } else {
                        java.time.Duration duration = java.time.Duration.between(now, targetTime);
                        long hours = duration.toHours();
                        long minutes = duration.toMinutesPart();
                        long seconds = duration.toSecondsPart();
                        lblTime.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                        if ("RUNNING".equals(currentItem.status) && hours == 0 && minutes < 10) {
                            lblTime.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 16px;");
                        } else {
                            lblTime.setStyle("-fx-background-color: " + finalBaseColor + "; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 16px;");
                        }
                    }
                }));
                timeline.setCycleCount(Timeline.INDEFINITE);
                timeline.play();
            } catch (Exception ex) { lblTime.setText("Lỗi giờ"); }
        } else {
            lblTime.setText("00:00:00");
            lblTime.setStyle("-fx-background-color: " + baseColor + "; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 16px;");
        }

        if (vboxItemDetails != null) {
            vboxItemDetails.getChildren().clear();
            if (currentItem.bidProduct != null && currentItem.bidProduct.itemType != null) {
                String type = currentItem.bidProduct.itemType;
                if ("ART".equals(type)) {
                    addDetailRow("Phân loại:", "Tác phẩm Nghệ thuật");
                    addDetailRow("Tác giả:", currentItem.bidProduct.nameAuthor);
                    addDetailRow("Năm sáng tác:", (currentItem.bidProduct.creationYear != null && currentItem.bidProduct.creationYear > 0) ? String.valueOf(currentItem.bidProduct.creationYear) : null);
                } else if ("ELECTRONIC".equals(type)) {
                    addDetailRow("Phân loại:", "Đồ Điện tử");
                    addDetailRow("Thương hiệu:", currentItem.bidProduct.brand);
                    addDetailRow("Bảo hành:", (currentItem.bidProduct.warrantyMonths != null && currentItem.bidProduct.warrantyMonths > 0) ? currentItem.bidProduct.warrantyMonths + " tháng" : null);
                } else if ("VEHICLE".equals(type)) {
                    addDetailRow("Phân loại:", "Phương tiện");
                    addDetailRow("Loại động cơ:", currentItem.bidProduct.engineType);
                    addDetailRow("Số Km đã đi:", (currentItem.bidProduct.mileage != null && currentItem.bidProduct.mileage >= 0) ? String.format("%,d Km", currentItem.bidProduct.mileage).replace(",", ".") : null);
                }
            }
        }

        if (txtDescription != null) txtDescription.setText(currentItem.bidProduct.description);

        if (currentItem.bidProduct.imageUrls != null && !currentItem.bidProduct.imageUrls.isEmpty()) this.imageUrls = currentItem.bidProduct.imageUrls;
        else this.imageUrls.clear();
        this.currentImageIndex = 0;
        updateImageView();

        if (currentItem.seller != null) {
            lblSellerName.setText(currentItem.seller.fullName != null ? currentItem.seller.fullName : currentItem.seller.userName);
            lblSellerRating.setText(currentItem.seller.rating + " / 5.0");
            lblSellerRating.setGraphic(createStar());
            if (currentItem.seller.avatarUrl != null) {
                imgSellerAvatar.setImage(new javafx.scene.image.Image(ApiService.BASE_URL + currentItem.seller.avatarUrl, true));
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(45, 45);
                clip.setArcWidth(45); clip.setArcHeight(45);
                imgSellerAvatar.setClip(clip);
            }
        }
    }

    private void updateImageView() {
        if (imageUrls.isEmpty()) {
            productImageView.setImage(new javafx.scene.image.Image("https://via.placeholder.com/200?text=No+Image"));
            if(lblImageIndex != null) lblImageIndex.setText("0/0");
            if(btnPrevImage != null) btnPrevImage.setDisable(true);
            if(btnNextImage != null) btnNextImage.setDisable(true);
        } else {
            productImageView.setImage(new javafx.scene.image.Image(ApiService.BASE_URL + imageUrls.get(currentImageIndex), true));
            if(lblImageIndex != null) lblImageIndex.setText((currentImageIndex + 1) + "/" + imageUrls.size());
            boolean hasMultiple = imageUrls.size() > 1;
            if(btnPrevImage != null) btnPrevImage.setDisable(!hasMultiple);
            if(btnNextImage != null) btnNextImage.setDisable(!hasMultiple);
        }
    }

    @FXML private void prevImage() {
        if (imageUrls.isEmpty()) return;
        currentImageIndex--;
        if (currentImageIndex < 0) currentImageIndex = imageUrls.size() - 1;
        updateImageView();
    }

    @FXML private void nextImage() {
        if (imageUrls.isEmpty()) return;
        currentImageIndex++;
        if (currentImageIndex >= imageUrls.size()) currentImageIndex = 0;
        updateImageView();
    }

    @FXML
    private void handleBidClick() {
        if (SessionManager.userName != null && currentItem.seller != null && SessionManager.userName.equals(currentItem.seller.userName)) return;
        String amountStr = txtBidAmount.getText().replace(".", "").replace(",", "").trim();
        if (amountStr.isEmpty()) return;
        try {
            double bidValue = Double.parseDouble(amountStr);
            if (bidValue <= currentItem.highestBid) { showToastError("Giá phải lớn hơn giá hiện tại!"); return; }
            lblConfirmAmount.setText(String.format("%,.0f VND", bidValue).replace(",", "."));
            paneConfirm.setVisible(true);
        } catch (Exception e) { showToastError("Số tiền không hợp lệ!"); }
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
                        initBidHistory(currentItem.id);
                        updateUI();
                        loadBalance();
                        showToastSuccess();
                        initBidData();
                    } else showToastError(apiRes.message);
                } else {
                    try { showToastError(ApiService.gson.fromJson(res.body(), ApiResponse.class).message); }
                    catch (Exception e) { showToastError("Lỗi hệ thống: " + res.statusCode()); }
                }
            });
        }).exceptionally(ex -> { Platform.runLater(() -> showToastError("Mất kết nối máy chủ!")); return null; });
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
        if (chartTimeline != null) chartTimeline.stop();
        try {
            Node view = FXMLLoader.load(getClass().getResource("/com/auction/view/auction/AuctionList.fxml"));
            StackPane contentArea = (StackPane) txtBidAmount.getScene().lookup("#contentArea");
            if(contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void addDetailRow(String label, String value) {
        String displayValue = (value == null || value.trim().isEmpty() || "null".equals(value)) ? "Đang cập nhật" : value;
        Label lblKey = new Label(label);
        lblKey.setStyle("-fx-text-fill: #7f8c8d; -fx-min-width: 110px; -fx-font-size: 14px;");
        Label lblValue = new Label(displayValue);
        lblValue.setWrapText(true);
        lblValue.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 14px;");
        vboxItemDetails.getChildren().add(new HBox(10, lblKey, lblValue));
    }

    private void initBidHistory(String auctionId) {
        if (chartTimeline != null) chartTimeline.stop();
        lblHistoryName.setText(currentItem.bidProduct.name);
        lblHistoryStartPrice.setText("Từ: " + String.format("%,.0f", currentItem.bidProduct.startPrice).replace(",", "."));

        chartTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            ApiService.getAsync("/auctions/" + auctionId + "/price-chart").thenAccept(res -> {
                Platform.runLater(() -> {
                    if (res.statusCode() == 200) {
                        ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        if (apiRes.code == 1000) {
                            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<AuctionModel.BidTransactionModel>>(){}.getType();
                            java.util.List<AuctionModel.BidTransactionModel> txs = ApiService.gson.fromJson(apiRes.result, listType);

                            double startPrice = currentItem.bidProduct.startPrice;
                            double maxPrice = startPrice;
                            vboxBidHistory.getChildren().clear();

                            if (txs != null && !txs.isEmpty()) {
                                txs.sort((t1, t2) -> Double.compare(t1.bidAmount, t2.bidAmount));
                                for (AuctionModel.BidTransactionModel tx : txs) if (tx.bidAmount > maxPrice) maxPrice = tx.bidAmount;

                                int displayCount = 0;
                                for (int i = txs.size() - 1; i >= 0; i--) {
                                    if (displayCount >= 5) break;
                                    vboxBidHistory.getChildren().add(createHistoryRow(txs.get(i)));
                                    displayCount++;
                                }
                            } else {
                                Label lblEmpty = new Label("Chưa có lượt đấu giá nào.");
                                lblEmpty.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic; -fx-padding: 10;");
                                vboxBidHistory.getChildren().add(lblEmpty);
                            }

                            double growthAmount = maxPrice - startPrice;
                            double percent = startPrice == 0 ? 0 : (growthAmount / startPrice) * 100;
                            double progress = startPrice == 0 ? 0 : Math.min(growthAmount / startPrice, 1.0);

                            String colorHex = (percent > 100) ? "#e74c3c" : "#2ecc71";
                            String percentText = (percent > 100) ? String.format("+%.1f%% ↑", percent) : String.format("+%.1f%%", percent);

                            lblHistoryCurrentPrice.setText("+" + String.format("%,.0f VND", growthAmount).replace(",", "."));
                            lblHistoryCurrentPrice.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: " + colorHex + ";");

                            lblHistoryPercent.setText(percentText);
                            lblHistoryPercent.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-min-width: 65px; -fx-alignment: center-right; -fx-text-fill: " + colorHex + ";");

                            bidProgressBar.setProgress(progress);
                            bidProgressBar.getStyleClass().removeAll("modern-progress-bar", "progress-alert", "progress-normal", "red-progress-bar");
                            bidProgressBar.getStyleClass().add(percent > 100 ? "progress-alert" : "progress-normal");

                            if (lblHighestBidder != null) lblHighestBidder.setText(getHighestBidderName(txs));
                            if (maxPrice > currentItem.highestBid) {
                                currentItem.highestBid = maxPrice;
                                lblCurrentPrice.setText(String.format("%,.0f VND", maxPrice).replace(",", "."));
                                initBidData();
                            }
                        }
                    }
                });
            });
        }));
        chartTimeline.setCycleCount(Timeline.INDEFINITE);
        chartTimeline.play();
    }

    private HBox createHistoryRow(AuctionModel.BidTransactionModel tx) {
        String time = tx.bidTimestamp.contains("T") ? tx.bidTimestamp.substring(11, 19) : tx.bidTimestamp;
        String user = (tx.bidder != null && tx.bidder.userName != null) ? tx.bidder.userName : "Ẩn danh";

        Label lblUser = new Label(user);
        lblUser.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 14px;");
        Label lblTime = new Label(time);
        lblTime.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 12px;");
        VBox leftBox = new VBox(2, lblUser, lblTime);
        leftBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblPrice = new Label(String.format("%,.0f đ", tx.bidAmount).replace(",", "."));
        lblPrice.setStyle("-fx-font-weight: bold; -fx-text-fill: #e67e22; -fx-font-size: 15px;");

        HBox row = new HBox(leftBox, spacer, lblPrice);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: white; -fx-padding: 10 15; -fx-background-radius: 8; -fx-border-color: #ecf0f1; -fx-border-radius: 8;");
        return row;
    }

    @FXML private void handleOpenImageZoom() {
        if (productImageView.getImage() != null) {
            zoomedImageView.setImage(productImageView.getImage());
            paneImageZoom.setVisible(true);
            if (chartTimeline != null) chartTimeline.pause();
        }
    }

    @FXML private void handleCloseImageZoom() {
        paneImageZoom.setVisible(false);
        zoomedImageView.setImage(null);
        if (chartTimeline != null) chartTimeline.play();
    }

    @FXML private void handleOpenChart() {
        if (timeline != null) timeline.stop();
        if (chartTimeline != null) chartTimeline.stop();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/auction/AuctionChart.fxml"));
            Node view = loader.load();
            AuctionChartController controller = loader.getController();
            controller.setAuctionData(currentItem);
            StackPane contentArea = (StackPane) lblBalance.getScene().lookup("#contentArea");
            if(contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleOpenHistory() {
        if (timeline != null) timeline.stop();
        if (chartTimeline != null) chartTimeline.stop();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/auction/AuctionHistory.fxml"));
            Node view = loader.load();
            AuctionHistoryController controller = loader.getController();
            controller.setAuctionData(currentItem);
            StackPane contentArea = (StackPane) lblBalance.getScene().lookup("#contentArea");
            if(contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleOpenSellerProfile() {
        if (timeline != null) timeline.stop();
        if (chartTimeline != null) chartTimeline.stop();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/profile/SellerProfile.fxml"));
            Node view = loader.load();
            SellerProfileController controller = loader.getController();
            controller.setSellerData(currentItem.seller, currentItem);
            StackPane contentArea = (StackPane) txtBidAmount.getScene().lookup("#contentArea");
            if(contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private SVGPath createStar() {
        SVGPath star = new SVGPath();
        star.setContent("M 8 0 L 10.46 5.36 L 16 6.24 L 12 10.36 L 12.94 16 L 8 13.24 L 3.06 16 L 4 10.36 L 0 6.24 L 5.54 5.36 Z");
        star.setFill(Color.web("#f39c12"));
        star.setStroke(Color.web("#f39c12"));
        star.setStrokeWidth(1.5);
        star.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return star;
    }

    @FXML
    public void toggleBalanceVisibility() {
        isBalanceHiddenDetail = !isBalanceHiddenDetail;

        if (isBalanceHiddenDetail) {
            lblBalance.setText("****** VND");
            eyeIconText.setText("Hiện");
        } else {
            lblBalance.setText(realBalanceTextDetail);
            eyeIconText.setText("Ẩn");
        }
    }
}
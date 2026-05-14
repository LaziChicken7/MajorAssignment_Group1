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
import javafx.scene.chart.XYChart;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.paint.Color;

public class AuctionDetailController {

    @FXML private Label lblId, lblName, lblTime, lblStartPrice, lblCurrentPrice, lblConfirmAmount, lblBalance;
    @FXML private Label lblMyBid;

    // BIẾN MỚI: Hiển thị tên người cao nhất
    @FXML private Label lblHighestBidder;

    @FXML private TextField txtBidAmount;
    @FXML private TextArea txtDescription;
    @FXML private VBox paneConfirm;
    @FXML private HBox toastSuccess, toastError;
    @FXML private Label lblToastErrorMsg;

    @FXML private Button btnBidAction;
    @FXML private Button btnAutoBidAction; // BỔ SUNG NÚT BOT

    @FXML private Label lblTimeTitle;
    @FXML private VBox vboxItemDetails; // Khung chứa thông tin động

    // --- CỦA BẠN 1: ẢNH & BIỂU ĐỒ ---
    @FXML private LineChart<String, Number> priceChart;
    @FXML private ImageView productImageView;
    @FXML private Label btnPrevImage, btnNextImage, lblImageIndex;
    private Timeline chartTimeline;
    private List<String> imageUrls = new ArrayList<>();
    private int currentImageIndex = 0;

    // --- CỦA BẠN 2: BƯỚC NHẢY TIỀN (STEPPER) ---
    private long currentBidValue = 0;
    private final long STEP_VALUE = 1000;
    private long currentHighestBid = 0;

    private AuctionModel currentItem;
    private Timeline timeline;

    // --- CÁC BIẾN MỚI CHO LỊCH SỬ & PROGRESS BAR ---
    @FXML private Label lblHistoryName, lblHistoryStartPrice, lblHistoryCurrentPrice, lblHistoryPercent;
    @FXML private ProgressBar bidProgressBar;
    @FXML private VBox vboxBidHistory;

    // --- BIẾN CHO TÍNH NĂNG PHÓNG TO ẢNH ---
    @FXML private VBox paneImageZoom;
    @FXML private ImageView zoomedImageView;

    // --- BIẾN CHO TÍNH NĂNG AUTO-BID ---
    @FXML private VBox paneAutoBidConfig;
    @FXML private TextField txtAutoBidMaxAmount;

    @FXML private ImageView imgSellerAvatar;
    @FXML private Label lblSellerName, lblSellerRating;

    @FXML
    public void initialize() {
        // Ràng buộc nhập số cho text field đấu giá thường
        if (txtBidAmount != null) {
            txtBidAmount.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null || newValue.isEmpty()) return;
                try {
                    String plainNumber = newValue.replace(".", "").replace(",", "");
                    if (!plainNumber.isEmpty()) {
                        currentBidValue = Long.parseLong(plainNumber);
                    }
                } catch (NumberFormatException e) {
                    txtBidAmount.setText(oldValue);
                }
            });
        }

        // Ràng buộc nhập số cho ô nhập tiền Bot
        if (txtAutoBidMaxAmount != null) {
            txtAutoBidMaxAmount.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null || newValue.isEmpty()) return;
                try {
                    String plainNumber = newValue.replace(".", "").replace(",", "");
                    if (!plainNumber.isEmpty()) {
                        long value = Long.parseLong(plainNumber);
                        txtAutoBidMaxAmount.setText(String.format("%,d", value).replace(",", "."));
                    }
                } catch (NumberFormatException e) {
                    txtAutoBidMaxAmount.setText(oldValue);
                }
            });
        }
    }

    public void setAuctionData(AuctionModel item) {
        this.currentItem = item;
        updateUI();
        loadBalance();

        // ĐỔI DÒNG NÀY
        initBidHistory(item.id);
        initBidData();

        // Xử lý bật/tắt cả 2 nút đấu giá theo trạng thái phiên
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

        // =======================================================
        // GHI ĐÈ GIAO DIỆN NẾU ĐÂY LÀ SẢN PHẨM CỦA CHÍNH MÌNH
        // =======================================================
        boolean isSeller = SessionManager.userName != null && item.seller != null
                && SessionManager.userName.equals(item.seller.userName);

        if (isSeller) {
            // 1. Không dùng setDisable(true) để tránh mất Hover chuột.
            // Dùng CSS làm mờ (opacity: 0.5) và đổi chuột thành mũi tên mặc định.
            btnBidAction.setStyle(btnBidAction.getStyle() + "-fx-opacity: 0.5; -fx-cursor: default;");
            if (btnAutoBidAction != null) {
                btnAutoBidAction.setStyle(btnAutoBidAction.getStyle() + "-fx-opacity: 0.5; -fx-cursor: default;");
            }

            // 2. Tạo Tooltip (Thông báo nổi) phong cách Dark Mode hiện đại
            Tooltip sellerTooltip = new Tooltip("Bạn không thể đấu giá sản phẩm của chính mình!");
            sellerTooltip.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-size: 13.5px; -fx-font-weight: bold; -fx-padding: 8 12; -fx-background-radius: 6; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 3);");

            // Chỉnh thời gian hiện lên cực nhanh (100 mili-giây)
            sellerTooltip.setShowDelay(Duration.millis(100));

            // Gắn nhãn nổi này vào 2 nút
            Tooltip.install(btnBidAction, sellerTooltip);
            if (btnAutoBidAction != null) Tooltip.install(btnAutoBidAction, sellerTooltip);
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

    // --- HÀM TÌM TÊN NGƯỜI ĐẤU GIÁ CAO NHẤT ---
    private String getHighestBidderName(List<AuctionModel.BidTransactionModel> txs) {
        if (txs == null || txs.isEmpty()) return "Chưa có";

        AuctionModel.BidTransactionModel highestTx = txs.get(0);
        for (AuctionModel.BidTransactionModel tx : txs) {
            if (tx.bidAmount > highestTx.bidAmount) {
                highestTx = tx;
            }
        }
        return (highestTx.bidder != null && highestTx.bidder.userName != null)
                ? highestTx.bidder.userName : "Ẩn danh";
    }

    private void updateUI() {
        if (currentItem == null) return;

        lblId.setText("SP: " + currentItem.bidProduct.id.substring(0, 4).toUpperCase());
        lblName.setText(currentItem.bidProduct.name);
        lblStartPrice.setText(String.format("%,.0f VND", currentItem.bidProduct.startPrice).replace(",", "."));
        lblCurrentPrice.setText(String.format("%,.0f VND", currentItem.highestBid).replace(",", "."));

        // Cập nhật tên người cược cao nhất
        if (lblHighestBidder != null) {
            lblHighestBidder.setText(getHighestBidderName(currentItem.bidTransactions));
        }

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

        // --- RENDER THÔNG TIN CHI TIẾT THEO PHÂN LOẠI ---
        if (vboxItemDetails != null) {
            vboxItemDetails.getChildren().clear();
            if (currentItem.bidProduct != null && currentItem.bidProduct.itemType != null) {
                String type = currentItem.bidProduct.itemType;

                if ("ART".equals(type)) {
                    addDetailRow("Phân loại:", "Tác phẩm Nghệ thuật");
                    addDetailRow("Tác giả:", currentItem.bidProduct.nameAuthor);
                    String yearStr = (currentItem.bidProduct.creationYear != null && currentItem.bidProduct.creationYear > 0)
                            ? String.valueOf(currentItem.bidProduct.creationYear) : null;
                    addDetailRow("Năm sáng tác:", yearStr);

                } else if ("ELECTRONIC".equals(type)) {
                    addDetailRow("Phân loại:", "Đồ Điện tử");
                    addDetailRow("Thương hiệu:", currentItem.bidProduct.brand);
                    String warrantyStr = (currentItem.bidProduct.warrantyMonths != null && currentItem.bidProduct.warrantyMonths > 0)
                            ? currentItem.bidProduct.warrantyMonths + " tháng" : null;
                    addDetailRow("Bảo hành:", warrantyStr);

                } else if ("VEHICLE".equals(type)) {
                    addDetailRow("Phân loại:", "Phương tiện");
                    addDetailRow("Loại động cơ:", currentItem.bidProduct.engineType);
                    String mileageStr = (currentItem.bidProduct.mileage != null && currentItem.bidProduct.mileage >= 0)
                            ? String.format("%,d Km", currentItem.bidProduct.mileage).replace(",", ".") : null;
                    addDetailRow("Số Km đã đi:", mileageStr);
                }
            }
        }

        if (txtDescription != null) txtDescription.setText(currentItem.bidProduct.description);

        if (currentItem.bidProduct.imageUrls != null && !currentItem.bidProduct.imageUrls.isEmpty()) {
            this.imageUrls = currentItem.bidProduct.imageUrls;
        } else {
            this.imageUrls.clear();
        }
        this.currentImageIndex = 0;
        updateImageView();

        if (currentItem.seller != null) {
            lblSellerName.setText(currentItem.seller.fullName != null ? currentItem.seller.fullName : currentItem.seller.userName);
            // Xóa cái emoji đi, thay bằng text và icon Vector
            lblSellerRating.setText(currentItem.seller.rating + " / 5.0");
            lblSellerRating.setGraphic(createStar()); // Gắn ngôi sao xịn vào

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
            String fullUrl = ApiService.BASE_URL + imageUrls.get(currentImageIndex);
            productImageView.setImage(new javafx.scene.image.Image(fullUrl, true));
            if(lblImageIndex != null) lblImageIndex.setText((currentImageIndex + 1) + "/" + imageUrls.size());

            boolean hasMultiple = imageUrls.size() > 1;
            if(btnPrevImage != null) btnPrevImage.setDisable(!hasMultiple);
            if(btnNextImage != null) btnNextImage.setDisable(!hasMultiple);
        }
    }

    @FXML
    private void prevImage() {
        if (imageUrls.isEmpty()) return;
        currentImageIndex--;
        if (currentImageIndex < 0) currentImageIndex = imageUrls.size() - 1;
        updateImageView();
    }

    @FXML
    private void nextImage() {
        if (imageUrls.isEmpty()) return;
        currentImageIndex++;
        if (currentImageIndex >= imageUrls.size()) currentImageIndex = 0;
        updateImageView();
    }

    public void initBidData() {
        if (currentItem != null) {
            this.currentHighestBid = (long) currentItem.highestBid;
            this.currentBidValue = this.currentHighestBid + STEP_VALUE;
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

    @FXML
    private void handleBidClick() {
        // Chặn ngầm không cho click (Không cần báo lỗi Toast nữa)
        if (SessionManager.userName != null && currentItem.seller != null
                && SessionManager.userName.equals(currentItem.seller.userName)) {
            return;
        }

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
                        // Gọi trực tiếp API vẽ lại biểu đồ để làm mới toàn bộ thay vì giả lập
                        initBidHistory(currentItem.id);
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

    // ==========================================
    // TÍNH NĂNG CÀI ĐẶT AUTOBID (BỔ SUNG MỚI)
    // ==========================================
    @FXML
    private void handleOpenAutoBid() {
        // Chặn ngầm không cho click (Không cần báo lỗi Toast nữa)
        if (SessionManager.userName != null && currentItem.seller != null
                && SessionManager.userName.equals(currentItem.seller.userName)) {
            return;
        }

        if (txtAutoBidMaxAmount != null) {
            // Gợi ý cho người dùng mức giá tối đa = giá hiện tại + 5 bước giá
            long suggestMax = (long) currentItem.highestBid + (STEP_VALUE * 5);
            txtAutoBidMaxAmount.setText(String.format("%,d", suggestMax).replace(",", "."));
        }
        paneAutoBidConfig.setVisible(true);
    }

    @FXML
    private void handleCloseAutoBid() {
        paneAutoBidConfig.setVisible(false);
    }

    @FXML
    private void processSetupAutoBid() {
        String amountStr = txtAutoBidMaxAmount.getText().replace(".", "").replace(",", "").trim();
        if (amountStr.isEmpty()) return;

        try {
            double maxAmount = Double.parseDouble(amountStr);
            if (maxAmount <= currentItem.highestBid + STEP_VALUE) {
                showToastError("Giá trần phải lớn hơn giá hiện tại!");
                return;
            }

            // Gọi API theo định dạng @RequestParam của Spring Boot (nối chuỗi vào URL)
            String url = "/auctions/" + currentItem.id + "/setup-autobid"
                    + "?username=" + SessionManager.userName
                    + "&maxAmount=" + maxAmount;

            // Vì RequestBody không cần, gửi null
            ApiService.postAsync(url, null).thenAccept(res -> {
                Platform.runLater(() -> {
                    paneAutoBidConfig.setVisible(false); // Ẩn popup
                    if (res.statusCode() >= 200 && res.statusCode() < 300) {
                        ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        if (apiRes.code == 1000) {
                            showToastSuccess();
                            // Load lại để thấy bot hoạt động
                            initBidHistory(currentItem.id);
                        } else {
                            showToastError(apiRes.message);
                        }
                    } else {
                        showToastError("Lỗi kích hoạt Bot: " + res.statusCode());
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

    // ==========================================

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
        // Thay vì return (bỏ qua), ta thay bằng chữ "Đang cập nhật" để giao diện không bị trống
        String displayValue = (value == null || value.trim().isEmpty() || "null".equals(value)) ? "Đang cập nhật" : value;

        Label lblKey = new Label(label);
        lblKey.setStyle("-fx-text-fill: #7f8c8d; -fx-min-width: 110px; -fx-font-size: 14px;");

        Label lblValue = new Label(displayValue);
        lblValue.setWrapText(true); // Bật tính năng tự xuống dòng nếu nội dung quá dài
        lblValue.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 14px;");

        HBox row = new HBox(10, lblKey, lblValue);
        vboxItemDetails.getChildren().add(row);
    }

    // --- HÀM REAL-TIME: CẬP NHẬT LỊCH SỬ VÀ PROGRESS BAR ---
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
                                // BỔ SUNG: Ép sắp xếp mảng theo giá từ thấp đến cao (Khắc phục lỗi Bot bắn trùng mili-giây)
                                txs.sort((t1, t2) -> Double.compare(t1.bidAmount, t2.bidAmount));

                                for (AuctionModel.BidTransactionModel tx : txs) {
                                    if (tx.bidAmount > maxPrice) maxPrice = tx.bidAmount;
                                }

                                // Vẽ 5 giao dịch gần nhất (từ cuối mảng duyệt ngược lên đầu)
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

                            // --- CẬP NHẬT GIAO DIỆN PROGRESS BAR (BỔ SUNG LOGIC MỚI) ---
                            double percent = 0;
                            if (startPrice > 0 && maxPrice >= startPrice) {
                                // Tính phần trăm TĂNG TRƯỞNG so với giá gốc.
                                // Ví dụ: Gốc 10k, Giá hiện tại 10k -> Tăng 0%
                                // Giá hiện tại 15k -> Tăng 50%
                                percent = ((maxPrice - startPrice) / startPrice) * 100.0;
                            }

                            double growthAmount = maxPrice - startPrice; // Số tiền chênh lệch
                            lblHistoryCurrentPrice.setText("+" + String.format("%,.0f VND", growthAmount).replace(",", "."));
                            lblHistoryPercent.setText(String.format("+%.1f%% ↑", percent));

                            // Logic đầy thanh: Giả sử giá tăng gấp đôi (+100%) thì thanh bar sẽ đầy (progress = 1.0)
                            double progressVal = Math.min(percent / 100.0, 1.0);
                            bidProgressBar.setProgress(progressVal);

                            // --- CẬP NHẬT REAL-TIME CHO CÁC KHỐI BÊN NGOÀI ---
                            if (lblHighestBidder != null) {
                                lblHighestBidder.setText(getHighestBidderName(txs));
                            }
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

    @FXML
    private void handleOpenImageZoom() {
        if (productImageView.getImage() != null) {
            zoomedImageView.setImage(productImageView.getImage());
            paneImageZoom.setVisible(true);
            if (chartTimeline != null) chartTimeline.pause();
        }
    }

    @FXML
    private void handleCloseImageZoom() {
        paneImageZoom.setVisible(false);
        zoomedImageView.setImage(null);
        if (chartTimeline != null) chartTimeline.play();
    }

    // ==========================================
    // CHUYỂN SANG TRANG BIỂU ĐỒ TRỰC TIẾP
    // ==========================================
    @FXML
    private void handleOpenChart() {
        // Dừng các tiến trình ngầm trước khi chuyển trang để đỡ tốn RAM
        if (timeline != null) timeline.stop();
        if (chartTimeline != null) chartTimeline.stop();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/auction/AuctionChart.fxml"));
            Node view = loader.load();

            // Lấy Controller của trang biểu đồ và ném Data của sản phẩm hiện tại sang đó
            AuctionChartController controller = loader.getController();
            controller.setAuctionData(currentItem);

            StackPane contentArea = (StackPane) lblBalance.getScene().lookup("#contentArea");
            if(contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // CHUYỂN SANG TRANG LỊCH SỬ DẠNG BẢNG
    // ==========================================
    @FXML
    private void handleOpenHistory() {
        if (timeline != null) timeline.stop();
        if (chartTimeline != null) chartTimeline.stop();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/auction/AuctionHistory.fxml"));
            Node view = loader.load();

            AuctionHistoryController controller = loader.getController();
            controller.setAuctionData(currentItem);

            StackPane contentArea = (StackPane) lblBalance.getScene().lookup("#contentArea");
            if(contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleOpenSellerProfile() {
        if (timeline != null) timeline.stop();
        if (chartTimeline != null) chartTimeline.stop();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/profile/SellerProfile.fxml"));
            Node view = loader.load();

            SellerProfileController controller = loader.getController();
            // Ném cả thằng Seller và Sản phẩm hiện tại sang trang kia để tí nữa biết đường quay lại
            controller.setSellerData(currentItem.seller, currentItem);

            StackPane contentArea = (StackPane) txtBidAmount.getScene().lookup("#contentArea");
            if(contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Hàm vẽ Ngôi sao Vector bo tròn siêu mượt
    private SVGPath createStar() {
        SVGPath star = new SVGPath();
        // Tọa độ hình ngôi sao chuẩn 16x16
        star.setContent("M 8 0 L 10.46 5.36 L 16 6.24 L 12 10.36 L 12.94 16 L 8 13.24 L 3.06 16 L 4 10.36 L 0 6.24 L 5.54 5.36 Z");
        star.setFill(Color.web("#f39c12")); // Tô màu cam vàng
        star.setStroke(Color.web("#f39c12")); // Viền màu cam vàng
        star.setStrokeWidth(1.5);
        star.setStrokeLineJoin(StrokeLineJoin.ROUND); // ĐÂY LÀ PHÉP THUẬT: Tự động bo tròn 5 góc nhọn của ngôi sao
        return star;
    }
}
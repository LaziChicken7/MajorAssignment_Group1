package com.auction.controller.auction;

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
                    txtBidAmount.setText(oldValue);
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
            if (currentItem.bidProduct.itemType != null) {
                String type = currentItem.bidProduct.itemType;

                if ("ART".equals(type)) {
                    addDetailRow("Phân loại:", "Tác phẩm Nghệ thuật");
                    addDetailRow("Tác giả:", currentItem.bidProduct.nameAuthor);
                    if(currentItem.bidProduct.creationYear != null) {
                        addDetailRow("Năm sáng tác:", String.valueOf(currentItem.bidProduct.creationYear));
                    }
                } else if ("ELECTRONIC".equals(type)) {
                    addDetailRow("Phân loại:", "Đồ Điện tử");
                    addDetailRow("Thương hiệu:", currentItem.bidProduct.brand);
                    if(currentItem.bidProduct.warrantyMonths != null) {
                        addDetailRow("Bảo hành:", currentItem.bidProduct.warrantyMonths + " tháng");
                    }
                } else if ("VEHICLE".equals(type)) {
                    addDetailRow("Phân loại:", "Phương tiện");
                    addDetailRow("Loại động cơ:", currentItem.bidProduct.engineType);
                    if(currentItem.bidProduct.mileage != null) {
                        addDetailRow("Số Km đã đi:", String.format("%,d Km", currentItem.bidProduct.mileage).replace(",", "."));
                    }
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
    }

    private void updateImageView() {
        if (imageUrls.isEmpty()) {
            productImageView.setImage(new javafx.scene.image.Image("https://via.placeholder.com/200?text=No+Image"));
            if(lblImageIndex != null) lblImageIndex.setText("0/0");

            // Khóa nút
            if(btnPrevImage != null) btnPrevImage.setDisable(true);
            if(btnNextImage != null) btnNextImage.setDisable(true);
        } else {
            String fullUrl = ApiService.BASE_URL + imageUrls.get(currentImageIndex);
            productImageView.setImage(new javafx.scene.image.Image(fullUrl, true));
            if(lblImageIndex != null) lblImageIndex.setText((currentImageIndex + 1) + "/" + imageUrls.size());

            boolean hasMultiple = imageUrls.size() > 1;
            // Nếu có nhiều hơn 1 ảnh -> Mở khóa (Disable = false)
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

    // Biến toàn cục giữ Data cho biểu đồ (Khai báo ở ngay trên hàm initChart)
    private XYChart.Series<String, Number> priceSeries;

    private void initChart(String auctionId) {
        if (chartTimeline != null) chartTimeline.stop();

        // 1. CHỈ KHỞI TẠO VÀ XÓA DỮ LIỆU BIỂU ĐỒ 1 LẦN DUY NHẤT KHI MỞ TRANG
        priceChart.getData().clear();
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá (VND)");
        priceChart.getData().add(priceSeries);

        // 2. VÒNG LẶP CHỈ CẬP NHẬT DATA, KHÔNG XÓA SERIES NỮA
        chartTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            ApiService.getAsync("/auctions/" + auctionId + "/price-chart").thenAccept(res -> {
                Platform.runLater(() -> {
                    if (res.statusCode() == 200) {
                        ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        if (apiRes.code == 1000) {
                            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<AuctionModel.BidTransactionModel>>(){}.getType();
                            java.util.List<AuctionModel.BidTransactionModel> txs = ApiService.gson.fromJson(apiRes.result, listType);

                            double maxPrice = 0;

                            // 3. Nếu không có giao dịch nào, đừng vẽ gì cả để tránh lỗi chấm rác
                            if (txs == null || txs.isEmpty()) {
                                return;
                            }

                            // 4. Nếu số lượng giao dịch từ Server khác số lượng điểm đang có trên biểu đồ
                            // Nghĩa là có người vừa bid thêm -> Chỉ vẽ lại những điểm bị thiếu
                            if (txs.size() > priceSeries.getData().size()) {
                                // Quét từ cái index chưa được vẽ
                                for (int i = priceSeries.getData().size(); i < txs.size(); i++) {
                                    AuctionModel.BidTransactionModel tx = txs.get(i);
                                    String timeLabel = tx.bidTimestamp.contains("T") ? tx.bidTimestamp.substring(11, 19) : tx.bidTimestamp;

                                    // Thêm điểm mới vào biểu đồ ĐANG TỒN TẠI
                                    priceSeries.getData().add(new XYChart.Data<>(timeLabel, tx.bidAmount));

                                    if(tx.bidAmount > maxPrice) maxPrice = tx.bidAmount;
                                }
                            }

                            // 5. Tìm giá lớn nhất trong toàn bộ list để hiển thị người thắng
                            for(AuctionModel.BidTransactionModel tx : txs) {
                                if(tx.bidAmount > maxPrice) maxPrice = tx.bidAmount;
                            }

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
        if (chartTimeline != null) chartTimeline.stop();
        try {
            Node view = FXMLLoader.load(getClass().getResource("/com/auction/view/auction/AuctionList.fxml"));
            StackPane contentArea = (StackPane) txtBidAmount.getScene().lookup("#contentArea");
            if(contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void addDetailRow(String label, String value) {
        if (value == null || value.isEmpty() || "null".equals(value)) return;
        Label lblKey = new Label(label);
        lblKey.setStyle("-fx-text-fill: #7f8c8d; -fx-min-width: 110px; -fx-font-size: 14px;");
        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 14px;");
        HBox row = new HBox(10, lblKey, lblValue);
        vboxItemDetails.getChildren().add(row);
    }

    // --- HÀM REAL-TIME: CẬP NHẬT LỊCH SỬ VÀ PROGRESS BAR ---
    private void initBidHistory(String auctionId) {
        if (chartTimeline != null) chartTimeline.stop();

        // Gán tên và giá gốc lúc vừa mở trang
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
                            double maxPrice = startPrice; // Mặc định cao nhất = giá khởi điểm

                            vboxBidHistory.getChildren().clear(); // Xóa cũ vẽ mới

                            if (txs != null && !txs.isEmpty()) {
                                // Tìm giá cao nhất
                                for (AuctionModel.BidTransactionModel tx : txs) {
                                    if (tx.bidAmount > maxPrice) maxPrice = tx.bidAmount;
                                }

                                // Vẽ danh sách lịch sử (vòng lặp ngược để người mới nhất nằm trên cùng)
                                for (int i = txs.size() - 1; i >= 0; i--) {
                                    vboxBidHistory.getChildren().add(createHistoryRow(txs.get(i)));
                                }
                            } else {
                                Label lblEmpty = new Label("Chưa có lượt đấu giá nào.");
                                lblEmpty.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic; -fx-padding: 10;");
                                vboxBidHistory.getChildren().add(lblEmpty);
                            }

                            // --- CẬP NHẬT GIAO DIỆN PROGRESS BAR ---
                            double percent = (maxPrice / startPrice) * 100;
                            lblHistoryCurrentPrice.setText("+" + String.format("%,.0f VND", maxPrice).replace(",", "."));
                            lblHistoryPercent.setText(String.format("+%.1f%% ↑", percent));

                            // Như bạn yêu cầu: Giá cao nhất hiện tại luôn là Full vạch (1.0)
                            bidProgressBar.setProgress(1.0);

                            // --- CẬP NHẬT REAL-TIME CHO CÁC KHỐI BÊN NGOÀI (CỦA NGƯỜI DÙNG KHÁC) ---
                            if (lblHighestBidder != null) {
                                lblHighestBidder.setText(getHighestBidderName(txs));
                            }
                            if (maxPrice > currentItem.highestBid) {
                                currentItem.highestBid = maxPrice;
                                lblCurrentPrice.setText(String.format("%,.0f VND", maxPrice).replace(",", "."));
                                initBidData(); // Cập nhật lại Stepper khuyến nghị
                            }
                        }
                    }
                });
            });
        }));
        chartTimeline.setCycleCount(Timeline.INDEFINITE);
        chartTimeline.play();
    }

    // --- HÀM TẠO 1 DÒNG TRONG DANH SÁCH LỊCH SỬ ---
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

    // ==========================================
    // TÍNH NĂNG PHÓNG TO ẢNH (LIGHTBOX)
    // ==========================================

    @FXML
    private void handleOpenImageZoom() {
        // Lấy chính bức ảnh đang hiển thị ở khung nhỏ ném sang khung to
        if (productImageView.getImage() != null) {
            zoomedImageView.setImage(productImageView.getImage());
            paneImageZoom.setVisible(true);

            // Tạm thời dừng timeline load biểu đồ để dồn tài nguyên render ảnh to cho mượt
            if (chartTimeline != null) chartTimeline.pause();
        }
    }

    @FXML
    private void handleCloseImageZoom() {
        paneImageZoom.setVisible(false);
        zoomedImageView.setImage(null); // Xóa ảnh để giải phóng RAM

        // Tiếp tục chạy lại timeline biểu đồ
        if (chartTimeline != null) chartTimeline.play();
    }
}
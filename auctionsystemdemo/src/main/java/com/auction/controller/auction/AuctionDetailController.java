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
    private LocalDateTime currentTargetTime;

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

                    if (apiRes.code == 1000 && apiRes.result != null) {
                        try {
                            long currentMax = 0;

                            // BẮT LỖI TẤT CẢ CÁC TRƯỜNG HỢP JSON TRẢ VỀ:
                            if (apiRes.result.isJsonPrimitive()) {
                                // TH1: Server trả về một con số trần trụi (Ví dụ: 500000)
                                currentMax = apiRes.result.getAsLong();
                            }
                            else if (apiRes.result.isJsonObject()) {
                                // TH2: Server trả về 1 cục JSON Object (Ví dụ: {"maxAmount": 500000, ...})
                                // Tự động dò tìm biến maxAmount bên trong cục đó!
                                com.google.gson.JsonObject jsonObj = apiRes.result.getAsJsonObject();
                                if (jsonObj.has("maxAmount")) {
                                    currentMax = jsonObj.get("maxAmount").getAsLong();
                                } else if (jsonObj.has("max_amount")) {
                                    currentMax = jsonObj.get("max_amount").getAsLong();
                                }
                            }

                            // NẾU TÌM THẤY SỐ TIỀN > 0 THÌ CẬP NHẬT LÊN GIAO DIỆN
                            if (currentMax > 0) {
                                lblCurrentBotStatus.setText("Trạng thái: Đang hoạt động");
                                lblCurrentBotStatus.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2ecc71;"); // Xanh lá

                                if (lblCurrentMaxBid != null) {
                                    lblCurrentMaxBid.setText("Mức trần đang cài: " + String.format("%,d", currentMax).replace(",", ".") + " VND");
                                    lblCurrentMaxBid.setVisible(true);
                                    lblCurrentMaxBid.setManaged(true);
                                }

                                txtAutoBidMaxAmount.setText(String.format("%,d", currentMax).replace(",", "."));
                                if (btnSubmitAutoBid != null) btnSubmitAutoBid.setText("Cập nhật Bot");
                            }
                        } catch (Exception e) {
                            // In ra Console để nếu vẫn xịt thì bạn copy dòng này gửi tôi xem cấu trúc Backend trả về là gì
                            System.err.println("❌ Lỗi Parse API Bot: " + e.getMessage());
                            System.err.println("❌ JSON thực tế từ Server: " + apiRes.result.toString());
                        }
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
        if (currentItem == null || currentItem.bidProduct == null) return;

        // 1. Cập nhật thông tin cơ bản
        String shortId = currentItem.bidProduct.id != null && currentItem.bidProduct.id.length() >= 4
                ? currentItem.bidProduct.id.substring(0, 4).toUpperCase()
                : "N/A";
        lblId.setText("SP: " + shortId);
        lblName.setText(currentItem.bidProduct.name);
        lblStartPrice.setText(String.format("%,.0f VND", currentItem.bidProduct.startPrice).replace(",", "."));
        lblCurrentPrice.setText(String.format("%,.0f VND", currentItem.highestBid).replace(",", "."));

        if (lblHighestBidder != null) {
            lblHighestBidder.setText(getHighestBidderName(currentItem.bidTransactions));
        }

        // 2. Tìm Giá của tôi (Thay vì gọi hàm ẩn, ta dùng thuật toán tìm trực tiếp cho an toàn)
        double myHighestBid = 0;
        if (currentItem.bidTransactions != null && SessionManager.userName != null) {
            for (AuctionModel.BidTransactionModel tx : currentItem.bidTransactions) {
                if (tx.bidder != null && SessionManager.userName.equalsIgnoreCase(tx.bidder.userName)) {
                    if (tx.bidAmount > myHighestBid) {
                        myHighestBid = tx.bidAmount;
                    }
                }
            }
        }
        if (lblMyBid != null) {
            lblMyBid.setText(String.format("%,.0f VND", myHighestBid).replace(",", "."));
        }

        // 3. Xử lý màu sắc dựa trên Trạng Thái
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

        // 4. KHỞI TẠO TIMELINE ĐẾM NGƯỢC THÔNG MINH
        if (timeline != null) timeline.stop();

        if (("RUNNING".equals(currentItem.status) && currentItem.endTime != null) ||
                ("OPEN".equals(currentItem.status) && currentItem.startTime != null)) {
            try {
                String targetTimeStr = "OPEN".equals(currentItem.status) ? currentItem.startTime : currentItem.endTime;

                // Dùng hàm parseTimeSafely (Đã tạo ở bài trước) để loại bỏ mili-giây rác
                currentTargetTime = parseTimeSafely(targetTimeStr);

                if (currentTargetTime == null) throw new Exception("Lỗi: Không thể phân tích thời gian!");

                String finalBaseColor = baseColor;
                timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                    LocalDateTime now = LocalDateTime.now();

                    // Nếu thời gian hiện tại đã vượt qua (hoặc bằng) thời gian kết thúc
                    if (now.isAfter(currentTargetTime) || now.isEqual(currentTargetTime)) {
                        lblTime.setText("00:00:00");
                        lblTime.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 16px;");

                        timeline.stop(); // Dừng đồng hồ lại

                        // Khóa chặt các nút bấm vì đã hết giờ
                        btnBidAction.setDisable(true);
                        btnBidAction.setText("Đã kết thúc");
                        txtBidAmount.setDisable(true);
                        if (btnAutoBidAction != null) btnAutoBidAction.setDisable(true);

                    } else {
                        // Tính toán thời gian còn lại
                        java.time.Duration duration = java.time.Duration.between(now, currentTargetTime);
                        long hours = duration.toHours();
                        long minutes = duration.toMinutesPart();
                        long seconds = duration.toSecondsPart();

                        lblTime.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));

                        // Đổi màu ĐỎ cảnh báo nếu chỉ còn dưới 10 phút (Trong phiên RUNNING)
                        if ("RUNNING".equals(currentItem.status) && hours == 0 && minutes < 10) {
                            lblTime.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 16px;");
                        } else {
                            lblTime.setStyle("-fx-background-color: " + finalBaseColor + "; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 16px;");
                        }
                    }
                }));
                timeline.setCycleCount(Timeline.INDEFINITE);
                timeline.play();

            } catch (Exception ex) {
                lblTime.setText("Lỗi định dạng giờ");
                ex.printStackTrace();
            }
        } else {
            lblTime.setText("00:00:00");
            lblTime.setStyle("-fx-background-color: " + baseColor + "; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 16px;");
        }

        // 5. Cập nhật các hàng chi tiết động (Nghệ thuật / Điện tử / Phương tiện)
        if (vboxItemDetails != null) {
            vboxItemDetails.getChildren().clear();
            if (currentItem.bidProduct.itemType != null) {
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

        // 6. Cập nhật Ảnh sản phẩm
        if (currentItem.bidProduct.imageUrls != null && !currentItem.bidProduct.imageUrls.isEmpty()) {
            this.imageUrls = currentItem.bidProduct.imageUrls;
        } else {
            this.imageUrls.clear();
        }
        this.currentImageIndex = 0;
        updateImageView();

        // 7. Cập nhật thông tin Người bán (Seller)
        if (currentItem.seller != null) {
            lblSellerName.setText(currentItem.seller.fullName != null ? currentItem.seller.fullName : currentItem.seller.userName);
            lblSellerRating.setText(currentItem.seller.rating + " / 5.0");
            lblSellerRating.setGraphic(createStar());

            if (currentItem.seller.avatarUrl != null) {
                String avatarUrl = currentItem.seller.avatarUrl;
                if (!avatarUrl.startsWith("/")) avatarUrl = "/" + avatarUrl;

                imgSellerAvatar.setImage(new javafx.scene.image.Image(ApiService.BASE_URL + avatarUrl, true));
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
            // SỬ DỤNG CLASS CACHE CỦA BẠN (Ảnh to hơn nên để size 400x400)
            String actualUrl = ApiService.BASE_URL + imageUrls.get(currentImageIndex);
            com.auction.util.ImageCacheUtils.loadImage(productImageView, actualUrl, 400, 400, "https://via.placeholder.com/200?text=Loading...");

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
        // Gắn class chuẩn của bạn cho chữ tiêu đề (Xám mờ trong Dark Mode)
        lblKey.getStyleClass().add("row-text-muted");
        lblKey.setStyle("-fx-min-width: 110px; -fx-font-size: 14px;");

        Label lblValue = new Label(displayValue);
        lblValue.setWrapText(true);
        // Gắn class chuẩn của bạn cho giá trị (Trắng rực rỡ trong Dark Mode)
        lblValue.getStyleClass().add("row-title-bold");
        lblValue.setStyle("-fx-font-size: 14px;");

        vboxItemDetails.getChildren().add(new HBox(10, lblKey, lblValue));
    }

    // ==============================================================
    // HÀM KHỞI TẠO LỊCH SỬ ĐẤU GIÁ (TỰ ĐỘNG LÀM MỚI MỖI 2 GIÂY)
    // Bao gồm: Cập nhật "Giá của tôi", Thanh Tiến Trình & Chống Snipe
    // ==============================================================
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
                                // 1. Cập nhật danh sách mới vào biến gốc
                                currentItem.bidTransactions = txs;

                                txs.sort((t1, t2) -> Double.compare(t1.bidAmount, t2.bidAmount));

                                double myNewHighestBid = 0; // Biến tìm giá của TÔI

                                for (AuctionModel.BidTransactionModel tx : txs) {
                                    // Tìm giá cao nhất tổng
                                    if (tx.bidAmount > maxPrice) {
                                        maxPrice = tx.bidAmount;
                                    }

                                    // TÌM GIÁ CAO NHẤT CỦA TÔI
                                    if (SessionManager.userName != null
                                            && tx.bidder != null
                                            && tx.bidder.userName != null
                                            && SessionManager.userName.equalsIgnoreCase(tx.bidder.userName)) {
                                        if (tx.bidAmount > myNewHighestBid) {
                                            myNewHighestBid = tx.bidAmount;
                                        }
                                    }
                                }

                                // 2. CẬP NHẬT LÊN MÀN HÌNH "GIÁ CỦA TÔI"
                                if (lblMyBid != null) {
                                    lblMyBid.setText(String.format("%,.0f VND", myNewHighestBid).replace(",", "."));
                                }

                                // 3. Hiển thị 5 người đặt giá gần nhất
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

                            // 4. CẬP NHẬT PROGRESS BAR VÀ THỐNG KÊ
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

                            // =========================================================
                            // 5. NẾU CÓ NGƯỜI VỪA ĐẶT GIÁ MỚI -> KIỂM TRA ANTI-SNIPE
                            // =========================================================
                            if (maxPrice > currentItem.highestBid) {
                                currentItem.highestBid = maxPrice;
                                lblCurrentPrice.setText(String.format("%,.0f VND", maxPrice).replace(",", "."));
                                initBidData();

                                System.out.println("⏳ ĐANG GỌI API KIỂM TRA ANTI-SNIPE: /auctions/" + auctionId);

                                ApiService.getAsync("/auctions/" + auctionId).thenAccept(infoRes -> {
                                    System.out.println("📡 HTTP Status trả về: " + infoRes.statusCode());

                                    if (infoRes.statusCode() == 200) {
                                        ApiResponse infoApiRes = ApiService.gson.fromJson(infoRes.body(), ApiResponse.class);
                                        if (infoApiRes.code == 1000) {
                                            AuctionModel updatedAuction = ApiService.gson.fromJson(infoApiRes.result, AuctionModel.class);

                                            Platform.runLater(() -> {
                                                LocalDateTime newEndTime = parseTimeSafely(updatedAuction.endTime);
                                                LocalDateTime oldEndTime = parseTimeSafely(currentItem.endTime);
                                                LocalDateTime nowTime = LocalDateTime.now();

                                                System.out.println("\n========= BẢNG BÁO CÁO ANTI-SNIPE =========");
                                                System.out.println("🕒 Giờ hiện tại (NOW) : " + nowTime);
                                                System.out.println("🛑 Giờ kết thúc (CŨ)  : " + oldEndTime);
                                                System.out.println("🆕 Giờ kết thúc (MỚI) : " + newEndTime);

                                                if (newEndTime != null && oldEndTime != null) {
                                                    boolean isAfter = newEndTime.isAfter(oldEndTime);
                                                    System.out.println("⚖️ So sánh (MỚI > CŨ) : " + isAfter);

                                                    if (isAfter) {
                                                        System.out.println("✅ THÀNH CÔNG: ĐÃ GIA HẠN THỜI GIAN!");
                                                        currentItem.endTime = updatedAuction.endTime;
                                                        currentTargetTime = newEndTime;

                                                        if (timeline != null && timeline.getStatus() != javafx.animation.Animation.Status.RUNNING) {
                                                            System.out.println("🔄 BẬT LẠI ĐỒNG HỒ VÀ MỞ KHÓA NÚT BẤM!");
                                                            timeline.play();
                                                            btnBidAction.setDisable(false);
                                                            btnBidAction.setText("Thủ công");
                                                            txtBidAmount.setDisable(false);
                                                            if (btnAutoBidAction != null) btnAutoBidAction.setDisable(false);
                                                        }

                                                        lblTime.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 16px;");
                                                    } else {
                                                        System.out.println("❌ THẤT BẠI: Thời gian mới từ Server không hề lớn hơn thời gian cũ.");
                                                    }
                                                } else {
                                                    System.out.println("❌ LỖI: Biến thời gian bị NULL!");
                                                }
                                                System.out.println("===========================================\n");
                                            });
                                        }
                                    } else {
                                        System.err.println("❌ LỖI GỌI API ANTI-SNIPE: Không lấy được thông tin từ Server. HTTP Status: " + infoRes.statusCode());
                                        System.err.println("Chi tiết lỗi: " + infoRes.body());
                                    }
                                }).exceptionally(ex -> {
                                    System.err.println("❌ LỖI MẠNG CHỐNG ANTI-SNIPE: " + ex.getMessage());
                                    return null;
                                });
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
        // FIX LỖI: Gắn class "row-title-bold" để chữ màu Đen (ở Light Mode) và Trắng (ở Dark Mode)
        lblUser.getStyleClass().add("row-title-bold");

        Label lblTime = new Label(time);
        // FIX LỖI: Gắn class "row-text-muted" để thời gian màu Xám (ở Light Mode) và Xám nhạt (ở Dark Mode)
        lblTime.getStyleClass().add("row-text-muted");

        VBox leftBox = new VBox(2, lblUser, lblTime);
        leftBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Màu cam của giá tiền luôn giữ nguyên vì nó nổi bật trên cả 2 nền
        Label lblPrice = new Label(String.format("%,.0f đ", tx.bidAmount).replace(",", "."));
        lblPrice.setStyle("-fx-font-weight: bold; -fx-text-fill: #e67e22; -fx-font-size: 15px;");

        HBox row = new HBox(leftBox, spacer, lblPrice);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 10 15;");

        // "custom-row" quyết định màu nền xám nhạt (Light) và xám đen (Dark)
        row.getStyleClass().add("custom-row");
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

    // ==============================================================
    // HÀM XỬ LÝ THỜI GIAN AN TOÀN CHỐNG CRASH
    // ==============================================================
    private LocalDateTime parseTimeSafely(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return null;
        try {
            timeStr = timeStr.contains("T") ? timeStr : timeStr.replace(" ", "T");

            // Cắt bỏ phần thập phân của giây (ví dụ: .198321)
            if (timeStr.contains(".")) timeStr = timeStr.substring(0, timeStr.indexOf("."));

            // Cắt bỏ múi giờ Z hoặc +07:00 nếu Server lỡ gửi kèm
            if (timeStr.endsWith("Z")) timeStr = timeStr.replace("Z", "");
            if (timeStr.contains("+")) timeStr = timeStr.substring(0, timeStr.indexOf("+"));

            return LocalDateTime.parse(timeStr);
        } catch (Exception e) {
            System.err.println("❌ LỖI PARSE THỜI GIAN: Chuỗi gốc [" + timeStr + "] - Lỗi: " + e.getMessage());
            return null;
        }
    }
}
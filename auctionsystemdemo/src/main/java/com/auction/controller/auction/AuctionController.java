package com.auction.controller.auction;

import com.auction.model.ApiResponse;
import com.auction.model.AuctionModel;
import com.auction.model.WalletDataResponse;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AuctionController {

    @FXML private ListView<AuctionModel> auctionListView;
    @FXML private Label lblBalance;
    @FXML private Label eyeIconText;
    @FXML private ComboBox<String> cbFilter;
    @FXML private ComboBox<String> cbSort;
    @FXML private VBox loadingOverlay;

    private List<AuctionModel> allAuctions = new ArrayList<>();
    private String realBalanceText = "0 VND";
    private boolean isHidden = true;

    // =======================================================
    // BIẾN LƯU TRỮ GIAO DIỆN RAM CACHE (CHỐNG GIẬT LAG)
    // =======================================================
    private Node cachedDetailView = null;
    private AuctionDetailController cachedDetailController = null;

    @FXML
    public void initialize() {
        cbFilter.setItems(FXCollections.observableArrayList(
                "Tất cả trạng thái", "Sắp diễn ra (OPEN)", "Đang diễn ra (RUNNING)",
                "Đã kết thúc (FINISHED)", "Đã thanh toán (PAID)", "Đã hủy (CANCELLED)"
        ));
        cbFilter.setValue("Đang diễn ra (RUNNING)");

        cbSort.setItems(FXCollections.observableArrayList(
                "Mặc định", "Kết thúc sớm nhất (Tăng dần)", "Kết thúc muộn nhất (Giảm dần)"
        ));
        cbSort.setValue("Kết thúc sớm nhất (Tăng dần)");

        cbFilter.setOnAction(e -> applyFilterAndSort());
        cbSort.setOnAction(e -> applyFilterAndSort());

        // 1. TỐI ƯU GIAO DIỆN DANH SÁCH (CHỐNG VÒNG LẶP VÔ TẬN)
        auctionListView.setCellFactory(param -> new ListCell<AuctionModel>() {
            private Node view;
            private AuctionItemController controller;

            // BIẾN NÀY LÀ CHÌA KHÓA CỨU SỐNG TOÀN BỘ APP CỦA BẠN
            private AuctionModel lastItem = null;

            {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/auction/AuctionItem.fxml"));
                    view = loader.load();
                    controller = loader.getController();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected void updateItem(AuctionModel item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    lastItem = null; // Reset cờ
                    if (controller != null) controller.setData(null);
                } else {
                    // ==============================================================
                    // BỨC TƯỜNG LỬA CHẶN VÒNG LẶP (IF LAST ITEM != ITEM)
                    // Chỉ nạp lại dữ liệu và tải ảnh NẾU ĐÂY LÀ MỘT SẢN PHẨM MỚI.
                    // Nếu JavaFX tự động gọi lại hàm này do giao diện bị co giãn,
                    // ta sẽ KHÔNG làm gì cả để cắt đứt vòng lặp!
                    // ==============================================================
                    if (this.lastItem != item) {
                        this.lastItem = item;
                        controller.setData(item); // Nạp data, tải ảnh, tính giờ...
                    }

                    setGraphic(view);
                }
            }
        });

        // Xử lý sự kiện click vào một phiên đấu giá
        auctionListView.setOnMouseClicked(event -> {
            AuctionModel selected = auctionListView.getSelectionModel().getSelectedItem();
            if (selected != null) showDetail(selected);
        });

        // 2. MA THUẬT NẰM Ở ĐÂY: TẢI TRƯỚC GIAO DIỆN CHI TIẾT NGAY KHI MỞ TRANG
        preloadDetailView();

        // 3. Bắt đầu tải dữ liệu Data
        loadData();
    }

    /**
     * Tải trước AuctionDetail.fxml ở luồng ngầm (Background Thread)
     * và nhét nó vào RAM. Người dùng chưa bấm thì nó đã nằm chờ sẵn!
     */
    private void preloadDetailView() {
        // Tải ở luồng UI nhưng ĐỢI 2 GIÂY sau khi trang mở xong mới tải,
        // để không tranh giành tài nguyên với lúc đang load danh sách.
        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
        delay.setOnFinished(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/auction/AuctionDetail.fxml"));
                this.cachedDetailView = loader.load(); // Load an toàn trên UI Thread
                this.cachedDetailController = loader.getController();
                System.out.println("✅ Đã Cache giao diện AuctionDetail vào RAM an toàn.");
            } catch (IOException ex) {
                System.err.println("❌ Lỗi khi tải trước AuctionDetail.fxml");
                ex.printStackTrace();
            }
        });
        delay.play();
    }

    @FXML
    public void loadData() {
        if (loadingOverlay != null) loadingOverlay.setVisible(true);

        // CÂU GIỜ 50ms ĐỂ VÒNG XOAY KỊP XUẤT HIỆN TRÊN MÀN HÌNH RỒI MỚI LÀM VIỆC NẶNG
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(50));
        pause.setOnFinished(event -> {

            // ĐẨY TOÀN BỘ VIỆC GỌI MẠNG VÀ PHÂN TÍCH JSON SANG LUỒNG NGẦM (BACKGROUND THREAD)
            CompletableFuture.runAsync(() -> {

                // 1. Load số dư ví ở luồng ngầm
                if (SessionManager.userName != null) {
                    try {
                        var res = ApiService.getAsync("/payments/" + SessionManager.userName + "/history").join();
                        if (res.statusCode() == 200) {
                            ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                            if (apiRes.code == 1000) {
                                WalletDataResponse wallet = ApiService.gson.fromJson(apiRes.result, WalletDataResponse.class);
                                realBalanceText = String.format("%,.0f VND", wallet.moneyOnWallet).replace(",", ".");
                                Platform.runLater(() -> {
                                    if (lblBalance != null) lblBalance.setText(isHidden ? "****** VND" : realBalanceText);
                                });
                            }
                        }
                    } catch (Exception ignored) {}
                }

                // 2. Load danh sách đấu giá ở luồng ngầm
                try {
                    // Nếu đã đăng nhập, gửi kèm tên user lên để Server tính sẵn mức giá bạn đang tham gia.
                    String url = "/auctions";
                    if (SessionManager.userName != null && !SessionManager.userName.isEmpty()) {
                        url += "?username=" + SessionManager.userName;
                    }
                    var res = ApiService.getAsync(url).join(); // Dùng .join() để chờ data tải xong ngay trong luồng ngầm
                    if (res.statusCode() == 200) {
                        ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        if (apiRes.code == 1000) {
                            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<AuctionModel>>(){}.getType();

                            // Dịch JSON sang List cũng ở luồng ngầm (Rất quan trọng nếu list dài)
                            List<AuctionModel> parsedList = ApiService.gson.fromJson(apiRes.result, listType);

                            // 3. Xong xuôi hết mới ném kết quả về cho UI
                            Platform.runLater(() -> {
                                allAuctions = parsedList;
                                applyFilterAndSort(); // Trả về UI để vẽ
                            });
                        } else {
                            Platform.runLater(() -> { if (loadingOverlay != null) loadingOverlay.setVisible(false); });
                        }
                    } else {
                        Platform.runLater(() -> { if (loadingOverlay != null) loadingOverlay.setVisible(false); });
                    }
                } catch (Exception ex) {
                    Platform.runLater(() -> { if (loadingOverlay != null) loadingOverlay.setVisible(false); });
                }
            });

        });

        pause.play(); // Bắt đầu chạy câu giờ
    }

    @FXML
    public void toggleBalanceVisibility() {
        isHidden = !isHidden;
        lblBalance.setText(isHidden ? "****** VND" : realBalanceText);
        eyeIconText.setText(isHidden ? "Hiện" : "Ẩn");
    }

    /**
     * Thuật toán lọc đã được gỡ bỏ Delay giả tạo.
     * Cập nhật danh sách bằng setAll() giúp bảng giữ nguyên các Cell cũ, không bị giật.
     */
    private void applyFilterAndSort() {
        if (allAuctions == null || allAuctions.isEmpty()) return;

        // 1. HIỆN LOADING VÀ ẨN DANH SÁCH TẠM THỜI
        if (loadingOverlay != null) loadingOverlay.setVisible(true);
        auctionListView.setOpacity(0); // Dùng opacity=0 thay vì setVisible(false) để giữ nguyên khung layout, chống giật

        // 2. NHƯỜNG LUỒNG UI 50ms: Giúp vòng xoay Spinner có thời gian bắt đầu quay
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(50));
        pause.setOnFinished(event -> {

            // 3. ĐẨY VIỆC LỌC RA LUỒNG NGẦM (BACKGROUND THREAD)
            CompletableFuture.supplyAsync(() -> {
                Stream<AuctionModel> stream = allAuctions.stream();

                String filterValue = cbFilter.getValue();
                if (filterValue != null && !filterValue.equals("Tất cả trạng thái")) {
                    if (filterValue.contains("OPEN")) stream = stream.filter(a -> "OPEN".equals(a.status));
                    else if (filterValue.contains("RUNNING")) stream = stream.filter(a -> "RUNNING".equals(a.status));
                    else if (filterValue.contains("FINISHED")) stream = stream.filter(a -> "FINISHED".equals(a.status));
                    else if (filterValue.contains("PAID")) stream = stream.filter(a -> "PAID".equals(a.status));
                    else if (filterValue.contains("CANCELLED")) stream = stream.filter(a -> "CANCELLED".equals(a.status));
                }

                String sortValue = cbSort.getValue();
                if ("Kết thúc sớm nhất (Tăng dần)".equals(sortValue) || "Kết thúc muộn nhất (Giảm dần)".equals(sortValue)) {
                    stream = stream.sorted((a1, a2) -> {
                        try {
                            String timeStr1 = (a1.endTime != null) ? a1.endTime.replace(" ", "T") : "9999-12-31T23:59:59";
                            String timeStr2 = (a2.endTime != null) ? a2.endTime.replace(" ", "T") : "9999-12-31T23:59:59";
                            LocalDateTime t1 = LocalDateTime.parse(timeStr1);
                            LocalDateTime t2 = LocalDateTime.parse(timeStr2);
                            return "Kết thúc sớm nhất (Tăng dần)".equals(sortValue) ? t1.compareTo(t2) : t2.compareTo(t1);
                        } catch (DateTimeParseException e) { return 0; }
                    });
                }

                return stream.collect(Collectors.toList());

            }).thenAccept(filteredList -> {
                // 4. TRỞ LẠI LUỒNG UI: NẠP DỮ LIỆU
                Platform.runLater(() -> {
                    if (auctionListView.getItems() == null) {
                        auctionListView.setItems(FXCollections.observableArrayList(filteredList));
                    } else {
                        auctionListView.getItems().setAll(filteredList);
                    }
                    if (!filteredList.isEmpty()) auctionListView.scrollTo(0);

                    // 5. CÂU GIỜ THÊM 100MS: Cho ListView khởi tạo xong xuôi toàn bộ Node ẩn
                    javafx.animation.PauseTransition showPause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(100));
                    showPause.setOnFinished(e -> {
                        if (loadingOverlay != null) loadingOverlay.setVisible(false); // Tắt Loading
                        auctionListView.setOpacity(1); // Hiển thị danh sách ra màn hình
                    });
                    showPause.play();
                });
            });
        });

        // Bắt đầu chu trình
        pause.play();
    }

    /**
     * Chuyển cảnh ngay lập tức trong 1 mili-giây (Instant Transition)
     */
    private void showDetail(AuctionModel item) {
        // KIỂM TRA XEM GIAO DIỆN ĐÃ ĐƯỢC CACHE CHƯA
        if (cachedDetailView == null || cachedDetailController == null) {
            System.out.println("⏳ Giao diện chi tiết đang tải, vui lòng thử lại sau giây lát...");
            return;
        }

        try {
            // 1. Đổ dữ liệu mới vào Controller cũ đã được load
            cachedDetailController.setAuctionData(item);

            // 2. Tìm khung chứa và ném cái View đã cache vào (Không cần Load lại ổ cứng)
            StackPane contentArea = (StackPane) auctionListView.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(cachedDetailView);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
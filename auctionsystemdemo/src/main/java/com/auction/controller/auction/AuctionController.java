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

        // ========================================================
        // TỐI ƯU SIÊU TỐC: CACHE FXML NODE ĐỂ KHÔNG BỊ GIẬT LAG
        // ========================================================
        auctionListView.setCellFactory(param -> new ListCell<AuctionModel>() {
            private Node view;
            private AuctionItemController controller;

            // Khối khởi tạo: Chỉ chạy 1 lần duy nhất khi tạo Cell
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
                } else {
                    // Tái sử dụng lại giao diện đã load, chỉ đắp data mới vào
                    controller.setData(item);
                    setGraphic(view);
                }
            }
        });

        auctionListView.setOnMouseClicked(event -> {
            AuctionModel selected = auctionListView.getSelectionModel().getSelectedItem();
            if (selected != null) showDetail(selected);
        });

        loadData();
    }

    @FXML
    public void loadData() {
        // 1. KHI BẮT ĐẦU TẢI: Hiện vòng xoay Loading lên che đi danh sách cũ
        if (loadingOverlay != null) loadingOverlay.setVisible(true);

        // Load số dư ví
        if (SessionManager.userName != null) {
            ApiService.getAsync("/payments/" + SessionManager.userName + "/history").thenAccept(res -> {
                Platform.runLater(() -> {
                    if (res.statusCode() == 200) {
                        ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        if (apiRes.code == 1000) {
                            WalletDataResponse wallet = ApiService.gson.fromJson(apiRes.result, WalletDataResponse.class);
                            realBalanceText = String.format("%,.0f VND", wallet.moneyOnWallet).replace(",", ".");
                            if (lblBalance != null) lblBalance.setText(isHidden ? "****** VND" : realBalanceText);
                        }
                    }
                });
            });
        }

        // 2. GỌI API LẤY DANH SÁCH ĐẤU GIÁ
        ApiService.getAsync("/auctions").thenAccept(res -> {
            // Dịch JSON ở luồng ngầm (Không làm đơ vòng xoay)
            if (res.statusCode() == 200) {
                ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);

                if (apiRes.code == 1000) {
                    java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<AuctionModel>>(){}.getType();
                    java.util.List<AuctionModel> parsedList = ApiService.gson.fromJson(apiRes.result, listType);

                    // 3. KHI TẢI XONG: Đẩy dữ liệu ra UI và TẮT vòng xoay đi
                    Platform.runLater(() -> {
                        allAuctions = parsedList;
                        applyFilterAndSort(); // Render danh sách

                        if (loadingOverlay != null) loadingOverlay.setVisible(false);
                    });
                } else {
                    Platform.runLater(() -> { if (loadingOverlay != null) loadingOverlay.setVisible(false); });
                }
            } else {
                Platform.runLater(() -> { if (loadingOverlay != null) loadingOverlay.setVisible(false); });
            }
        }).exceptionally(ex -> {
            Platform.runLater(() -> { if (loadingOverlay != null) loadingOverlay.setVisible(false); });
            return null;
        });
    }

    @FXML
    public void toggleBalanceVisibility() {
        isHidden = !isHidden;
        lblBalance.setText(isHidden ? "****** VND" : realBalanceText);
        eyeIconText.setText(isHidden ? "Hiện" : "Ẩn");
    }

    private void applyFilterAndSort() {
        if (allAuctions == null || allAuctions.isEmpty()) return;

        // 1. HIỆN LOADING, ẨN DANH SÁCH LẬP TỨC
        if (loadingOverlay != null) loadingOverlay.setVisible(true);
        auctionListView.setVisible(false);

        // 2. NHƯỜNG LUỒNG CHO UI VẼ VÒNG XOAY (Nghỉ 50ms)
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(50));
        pause.setOnFinished(event -> {

            // 3. ĐẨY VIỆC LỌC & SẮP XẾP SANG LUỒNG NGẦM (BACKGROUND THREAD)
            // Nhờ đó vòng xoay Loading vẫn quay mượt mà không bị đơ
            java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                java.util.stream.Stream<AuctionModel> stream = allAuctions.stream();

                // Logic lọc trạng thái
                String filterValue = cbFilter.getValue();
                if (filterValue != null && !filterValue.equals("Tất cả trạng thái")) {
                    if (filterValue.contains("OPEN")) stream = stream.filter(a -> "OPEN".equals(a.status));
                    else if (filterValue.contains("RUNNING")) stream = stream.filter(a -> "RUNNING".equals(a.status));
                    else if (filterValue.contains("FINISHED")) stream = stream.filter(a -> "FINISHED".equals(a.status));
                    else if (filterValue.contains("PAID")) stream = stream.filter(a -> "PAID".equals(a.status));
                    else if (filterValue.contains("CANCELLED")) stream = stream.filter(a -> "CANCELLED".equals(a.status));
                }

                // Logic sắp xếp thời gian
                String sortValue = cbSort.getValue();
                if ("Kết thúc sớm nhất (Tăng dần)".equals(sortValue) || "Kết thúc muộn nhất (Giảm dần)".equals(sortValue)) {
                    stream = stream.sorted((a1, a2) -> {
                        try {
                            String timeStr1 = (a1.endTime != null) ? a1.endTime.replace(" ", "T") : "9999-12-31T23:59:59";
                            String timeStr2 = (a2.endTime != null) ? a2.endTime.replace(" ", "T") : "9999-12-31T23:59:59";
                            java.time.LocalDateTime t1 = java.time.LocalDateTime.parse(timeStr1);
                            java.time.LocalDateTime t2 = java.time.LocalDateTime.parse(timeStr2);
                            return "Kết thúc sớm nhất (Tăng dần)".equals(sortValue) ? t1.compareTo(t2) : t2.compareTo(t1);
                        } catch (java.time.format.DateTimeParseException e) { return 0; }
                    });
                }

                // Trả về danh sách đã được tính toán xong
                return stream.collect(java.util.stream.Collectors.toList());

            }).thenAccept(filteredList -> {
                // 4. KẾT THÚC: Cập nhật lại UI, tắt Loading và hiện Danh sách
                Platform.runLater(() -> {
                    auctionListView.setItems(javafx.collections.FXCollections.observableArrayList(filteredList));
                    if (loadingOverlay != null) loadingOverlay.setVisible(false);
                    auctionListView.setVisible(true);
                });
            });

        });

        // Bắt đầu chạy luồng
        pause.play();
    }

    private void showDetail(AuctionModel item) {
        // 1. Bật icon loading xoay xoay lên ngay lập tức
        loadingOverlay.setVisible(true);

        // 2. Dùng PauseTransition nhường cho UI 50ms để vẽ vòng xoay loading lên màn hình
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(50));
        pause.setOnFinished(e -> {
            try {
                // Giao diện đã hiện Loading xong -> Bắt đầu nạp trang chi tiết
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/auction/AuctionDetail.fxml"));
                Node detailView = loader.load();
                AuctionDetailController controller = loader.getController();
                controller.setAuctionData(item);

                // Chuyển cảnh
                StackPane contentArea = (StackPane) auctionListView.getScene().lookup("#contentArea");
                if (contentArea != null) {
                    contentArea.getChildren().setAll(detailView);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                // 3. Nạp xong thì tắt Loading đi
                loadingOverlay.setVisible(false);
            }
        });
        pause.play();
    }
}
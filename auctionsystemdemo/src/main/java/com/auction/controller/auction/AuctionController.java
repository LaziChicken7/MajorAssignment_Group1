package com.auction.controller.auction;


import lombok.extern.slf4j.Slf4j;
import com.auction.model.ApiResponse;
import com.auction.model.AuctionModel;
import com.auction.model.WalletDataResponse;
import com.auction.util.ApiService;
import com.auction.util.GlobalWebSocketManager;
import com.auction.util.SessionManager;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
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

    private Node cachedDetailView = null;
    private AuctionDetailController cachedDetailController = null;

    // BỘ ĐẾM CHỜ CHỐNG SPAM WEBSOCKET
    private PauseTransition wsDebouncer = new PauseTransition(Duration.millis(400));

    @FXML
    public void initialize() {
        log.info("\u25B6 Controller Action - Execute: initialize()");
        cbFilter.setItems(FXCollections.observableArrayList(
                "Tất cả trạng thái", "Sắp diễn ra (OPEN)", "Đang diễn ra (RUNNING)",
                "Đã kết thúc (FINISHED)", "Đã thanh toán (PAID)", "Đã hủy (CANCELLED)"
        ));
        cbFilter.setValue("Đang diễn ra (RUNNING)");

        cbSort.setItems(FXCollections.observableArrayList(
                "Mặc định", "Kết thúc sớm nhất (Tăng dần)", "Kết thúc muộn nhất (Giảm dần)"
        ));
        cbSort.setValue("Kết thúc sớm nhất (Tăng dần)");

        cbFilter.setOnAction(e -> applyFilterAndSort(true));
        cbSort.setOnAction(e -> applyFilterAndSort(true));

        auctionListView.setCellFactory(param -> new ListCell<AuctionModel>() {
            private Node view;
            private AuctionItemController controller;
            private AuctionModel lastItem = null;

            {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/auction/AuctionItem.fxml"));
                    view = loader.load();
                    controller = loader.getController();
                } catch (IOException e) { log.error("Exception occurred", e); }
            }

            @Override
            protected void updateItem(AuctionModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    lastItem = null;
                    if (controller != null) controller.setData(null);
                } else {
                    // Dù là item cũ, nhưng nếu giá tiền (highestBid) hoặc trạng thái thay đổi, ta cũng bắt nó update lại!
                    if (this.lastItem != item || this.lastItem.highestBid != item.highestBid || !this.lastItem.status.equals(item.status)) {
                        this.lastItem = item;
                        controller.setData(item);
                    }
                    setGraphic(view);
                }
            }
        });

        auctionListView.setOnMouseClicked(event -> {
            AuctionModel selected = auctionListView.getSelectionModel().getSelectedItem();
            if (selected != null) showDetail(selected);
        });

        // 1. Tải trước giao diện
        preloadDetailView();

        // 2. Load dữ liệu lần đầu tiên (Có vòng xoay loading)
        loadData();

        // =================================================================
        // 3. WEBSOCKET REAL-TIME: KẾT NỐI VÀ LẮNG NGHE SỰ THAY ĐỔI
        // =================================================================
        GlobalWebSocketManager.listenToGlobalAuctions(() -> {
            Platform.runLater(() -> {
                wsDebouncer.setOnFinished(e -> {
                    log.info("⚡ WS GLOBAL: Đã cập nhật xong, tải lại ngầm...");
                    loadDataSilently();
                });
                wsDebouncer.playFromStart();
            });
        });

        // Hủy lắng nghe khi màn hình này bị đóng (Chuyển trang)
        auctionListView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) GlobalWebSocketManager.stopListeningGlobalAuctions();
        });
    }

    private void preloadDetailView() {
        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
        delay.setOnFinished(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/auction/AuctionDetail.fxml"));
                this.cachedDetailView = loader.load();
                this.cachedDetailController = loader.getController();
            } catch (IOException ex) { log.error("Exception occurred", ex); }
        });
        delay.play();
    }

    // ==================================================
    // LOAD DỮ LIỆU CÓ HIỆU ỨNG (Dùng khi mới vào trang)
    // ==================================================
    @FXML
    public void loadData() {
        log.info("\u25B6 Controller Action - Execute: loadData()");
        if (loadingOverlay != null) loadingOverlay.setVisible(true);
        fetchDataFromServer(true);
    }

    // ==================================================
    // LOAD DỮ LIỆU NGẦM (Dùng cho WebSocket, không bị chớp giật)
    // ==================================================
    public void loadDataSilently() {
        log.info("\u25B6 Controller Action - Execute: loadDataSilently()");
        fetchDataFromServer(false);
    }

    // HÀM LÕI LẤY DỮ LIỆU
    private void fetchDataFromServer(boolean showLoading) {
        CompletableFuture.runAsync(() -> {
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

            try {
                String url = "/auctions";
                if (SessionManager.userName != null && !SessionManager.userName.isEmpty()) {
                    url += "?username=" + SessionManager.userName;
                }
                var res = ApiService.getAsync(url).join();
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<AuctionModel>>(){}.getType();
                        List<AuctionModel> parsedList = ApiService.gson.fromJson(apiRes.result, listType);

                        Platform.runLater(() -> {
                            allAuctions = parsedList;
                            applyFilterAndSort(showLoading);
                        });
                    } else if (showLoading) hideLoading();
                } else if (showLoading) hideLoading();
            } catch (Exception ex) { if (showLoading) hideLoading(); }
        });
    }

    private void hideLoading() {
        Platform.runLater(() -> {
            if (loadingOverlay != null) loadingOverlay.setVisible(false);
        });
    }

    @FXML
    public void toggleBalanceVisibility() {
        isHidden = !isHidden;
        lblBalance.setText(isHidden ? "****** VND" : realBalanceText);
        eyeIconText.setText(isHidden ? "Hiện" : "Ẩn");
    }

    // THUẬT TOÁN LỌC: Tham số showLoading để quyết định xem có chớp nháy màn hình hay không
    private void applyFilterAndSort(boolean showLoading) {
        if (allAuctions == null || allAuctions.isEmpty()) {
            Platform.runLater(() -> {
                auctionListView.getItems().clear();
                if (loadingOverlay != null) loadingOverlay.setVisible(false);
                auctionListView.setOpacity(1);
            });
            return;
        }

        if (showLoading) {
            if (loadingOverlay != null) loadingOverlay.setVisible(true);
            auctionListView.setOpacity(0);
        }

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
            Platform.runLater(() -> {
                // LƯU LẠI VỊ TRÍ CUỘN HIỆN TẠI (Để khi tải ngầm không bị nhảy giật lên đầu)
                double currentScrollPosition = 0;
                if (!showLoading && !auctionListView.getItems().isEmpty()) {
                    currentScrollPosition = auctionListView.lookup(".scroll-bar") != null ?
                            ((ScrollBar) auctionListView.lookup(".scroll-bar:vertical")).getValue() : 0;
                }

                auctionListView.getItems().setAll(filteredList);

                if (showLoading) {
                    if (!filteredList.isEmpty()) auctionListView.scrollTo(0);
                    javafx.animation.PauseTransition showPause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(100));
                    showPause.setOnFinished(e -> {
                        if (loadingOverlay != null) loadingOverlay.setVisible(false);
                        auctionListView.setOpacity(1);
                    });
                    showPause.play();
                } else {
                    // Trả lại vị trí cuộn cũ
                    final double scrollPos = currentScrollPosition;
                    Platform.runLater(() -> {
                        if (auctionListView.lookup(".scroll-bar:vertical") != null) {
                            ((ScrollBar) auctionListView.lookup(".scroll-bar:vertical")).setValue(scrollPos);
                        }
                    });
                }
            });
        });
    }

    private void showDetail(AuctionModel item) {
        log.info("\u25B6 Controller Action - Execute: showDetail()");
        GlobalWebSocketManager.stopListeningGlobalAuctions(); // Gỡ kết nối khi chuyển trang
        if (cachedDetailView == null || cachedDetailController == null) return;
        try {
            cachedDetailController.setAuctionData(item);
            StackPane contentArea = (StackPane) auctionListView.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(cachedDetailView);
            }
        } catch (Exception ex) { log.error("Exception occurred", ex); }
    }
}
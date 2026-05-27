package com.auction.controller.addauctionitem;

import com.auction.controller.auction.AuctionDetailController;
import com.auction.model.ApiResponse;
import com.auction.model.AuctionModel;
import com.auction.model.WalletDataResponse;
import com.auction.util.ApiService;
import com.auction.util.GlobalWebSocketManager;
import com.auction.util.SessionManager;
import com.google.gson.reflect.TypeToken;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MyAuctionListController {

    @FXML private ListView<AuctionModel> myAuctionListView;
    @FXML private Label lblBalance;
    @FXML private Label eyeIconText;
    @FXML private VBox loadingOverlay;

    private String realBalanceTextDetail = "0 VND";
    private boolean isBalanceHiddenDetail = true;
    private List<AuctionModel> allMyAuctions = new ArrayList<>();

    // BỘ ĐẾM CHỜ CHỐNG SPAM WEBSOCKET
    private PauseTransition wsDebouncer = new PauseTransition(Duration.millis(400));

    @FXML
    public void initialize() {
        myAuctionListView.setCellFactory(param -> new ListCell<AuctionModel>() {
            private Node view;
            private MyAuctionItemController controller;
            private AuctionModel lastItem = null;

            {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/auction/MyAuctionItem.fxml"));
                    view = loader.load();
                    controller = loader.getController();
                } catch (IOException e) { e.printStackTrace(); }
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
                    // Update nếu là item mới HOẶC giá/trạng thái có thay đổi
                    if (this.lastItem != item || this.lastItem.highestBid != item.highestBid || !this.lastItem.status.equals(item.status)) {
                        this.lastItem = item;
                        controller.setData(item);
                    }
                    setGraphic(view);
                }
            }
        });

        myAuctionListView.setOnMouseClicked(event -> {
            AuctionModel selected = myAuctionListView.getSelectionModel().getSelectedItem();
            if (selected != null) showProductDetail(selected);
        });

        // 1. Tải lần đầu (Có vòng xoay Loading)
        loadData();

        // =================================================================
        // 2. WEBSOCKET REAL-TIME: NGỒI CHỜ SERVER BÁO CÓ NGƯỜI ĐẶT GIÁ MỚI
        // =================================================================
        GlobalWebSocketManager.listenToGlobalAuctions(() -> {
            Platform.runLater(() -> {
                wsDebouncer.setOnFinished(e -> {
                    System.out.println("⚡ WS MY-AUCTIONS: Có biến! Đang tải lại ngầm...");
                    loadDataSilently();
                });
                wsDebouncer.playFromStart();
            });
        });

        // Gỡ lắng nghe khi chuyển trang
        myAuctionListView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) GlobalWebSocketManager.stopListeningGlobalAuctions();
        });
    }

    @FXML
    public void loadData() {
        if (loadingOverlay != null) loadingOverlay.setVisible(true);
        myAuctionListView.setOpacity(0);
        fetchDataFromServer(true);
    }

    private void loadDataSilently() {
        fetchDataFromServer(false);
    }

    private void fetchDataFromServer(boolean showLoading) {
        String currentUser = SessionManager.userName;
        if (currentUser == null) return;

        CompletableFuture.runAsync(() -> {
            try {
                // Tải số dư
                var balanceReq = ApiService.getAsync("/payments/" + currentUser + "/history");
                var auctionsReq = ApiService.getAsync("/auctions/my-auctions?username=" + currentUser);

                CompletableFuture.allOf(balanceReq, auctionsReq).join();

                // Xử lý số dư
                var resBalance = balanceReq.get();
                if (resBalance.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(resBalance.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        WalletDataResponse wallet = ApiService.gson.fromJson(apiRes.result, WalletDataResponse.class);
                        Platform.runLater(() -> {
                            if (lblBalance != null) {
                                realBalanceTextDetail = String.format("%,.0f VND", wallet.moneyOnWallet).replace(",", ".");
                                lblBalance.setText(isBalanceHiddenDetail ? "****** VND" : realBalanceTextDetail);
                            }
                        });
                    }
                }

                // Xử lý Danh sách
                var resAuctions = auctionsReq.get();
                if (resAuctions.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(resAuctions.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        java.lang.reflect.Type listType = new TypeToken<List<AuctionModel>>(){}.getType();
                        List<AuctionModel> myAuctions = ApiService.gson.fromJson(apiRes.result, listType);

                        Platform.runLater(() -> {
                            allMyAuctions = myAuctions;

                            if (allMyAuctions.isEmpty()) {
                                myAuctionListView.getItems().clear();
                                if (loadingOverlay != null) loadingOverlay.setVisible(false);
                                myAuctionListView.setOpacity(1);
                                return;
                            }

                            // Giữ nguyên vị trí thanh cuộn khi tải ngầm
                            double scrollPos = 0;
                            if (!showLoading && !myAuctionListView.getItems().isEmpty()) {
                                Node scrollBar = myAuctionListView.lookup(".scroll-bar:vertical");
                                if (scrollBar instanceof ScrollBar) scrollPos = ((ScrollBar) scrollBar).getValue();
                            }

                            myAuctionListView.getItems().setAll(allMyAuctions);

                            if (showLoading) {
                                javafx.animation.PauseTransition showPause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(100));
                                showPause.setOnFinished(e -> {
                                    if (loadingOverlay != null) loadingOverlay.setVisible(false);
                                    myAuctionListView.setOpacity(1);
                                });
                                showPause.play();
                            } else {
                                final double finalPos = scrollPos;
                                Platform.runLater(() -> {
                                    Node scrollBar = myAuctionListView.lookup(".scroll-bar:vertical");
                                    if (scrollBar instanceof ScrollBar) ((ScrollBar) scrollBar).setValue(finalPos);
                                });
                            }
                        });
                    } else if (showLoading) hideLoading();
                } else if (showLoading) hideLoading();

            } catch (Exception e) {
                e.printStackTrace();
                if (showLoading) hideLoading();
            }
        });
    }

    private void hideLoading() {
        Platform.runLater(() -> {
            if (loadingOverlay != null) loadingOverlay.setVisible(false);
            myAuctionListView.setOpacity(1);
        });
    }

    private void showProductDetail(AuctionModel item) {
        GlobalWebSocketManager.stopListeningGlobalAuctions(); // Ngắt cáp mạng khi qua trang chi tiết
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/auction/AuctionDetail.fxml"));
            Node view = loader.load();
            AuctionDetailController controller = loader.getController();
            controller.setAuctionData(item);
            StackPane contentArea = (StackPane) myAuctionListView.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    public void goToAddProduct() {
        if (SessionManager.userName == null) return;
        GlobalWebSocketManager.stopListeningGlobalAuctions();

        if (loadingOverlay != null) loadingOverlay.setVisible(true);

        ApiService.getAsync("/users/profile/" + SessionManager.userName).thenAccept(res -> {
            Platform.runLater(() -> {
                if (loadingOverlay != null) loadingOverlay.setVisible(false);

                if (res.statusCode() >= 200 && res.statusCode() < 300) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        com.auction.model.UserModel currentUser = ApiService.gson.fromJson(apiRes.result, com.auction.model.UserModel.class);
                        SessionManager.role = currentUser.role;
                    }
                }

                if ("SELLER".equals(SessionManager.role) || "ADMIN".equals(SessionManager.role)) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/addauctionitem/AddProduct.fxml"));
                        Node view = loader.load();
                        StackPane contentArea = (StackPane) myAuctionListView.getScene().lookup("#contentArea");
                        if (contentArea != null) contentArea.getChildren().setAll(view);
                    } catch (Exception e) { e.printStackTrace(); }
                } else {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                    alert.setTitle("Từ chối truy cập");
                    alert.setHeaderText(null);
                    alert.setContentText("Bạn chưa có quyền SELLER để đưa sản phẩm lên sàn!\nVui lòng liên hệ Admin để được nâng cấp tài khoản.");
                    com.auction.util.AlertUtils.applyStyle(alert);
                    alert.showAndWait();
                }
            });
        });
    }

    @FXML
    public void toggleBalanceVisibility() {
        isBalanceHiddenDetail = !isBalanceHiddenDetail;
        lblBalance.setText(isBalanceHiddenDetail ? "****** VND" : realBalanceTextDetail);
        eyeIconText.setText(isBalanceHiddenDetail ? "Hiện" : "Ẩn");
    }
}
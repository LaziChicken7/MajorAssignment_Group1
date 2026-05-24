package com.auction.controller.addauctionitem;

import com.auction.controller.auction.AuctionDetailController;
import com.auction.model.ApiResponse;
import com.auction.model.AuctionModel;
import com.auction.model.WalletDataResponse;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MyAuctionListController {

    @FXML private ListView<AuctionModel> myAuctionListView;
    @FXML private Label lblBalance;
    @FXML private Label eyeIconText;

    // THÊM BIẾN NÀY ĐỂ HIỂN THỊ VÒNG XOAY LOADING
    @FXML private VBox loadingOverlay;

    private String realBalanceTextDetail = "0 VND";
    private boolean isBalanceHiddenDetail = true;

    @FXML
    public void initialize() {
        // ========================================================
        // TỐI ƯU SIÊU TỐC: CACHE FXML NODE VÀ CHỐNG VÒNG LẶP VÔ TẬN
        // ========================================================
        myAuctionListView.setCellFactory(param -> new ListCell<AuctionModel>() {
            private Node view;
            private MyAuctionItemController controller;
            private AuctionModel lastItem = null; // CHÌA KHÓA CHỐNG GIẬT LAG

            {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/auction/MyAuctionItem.fxml"));
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
                    // CÁCH TỐI ƯU NHẤT: Chỉ cần giấu nguyên cái khung FXML đi là xong.
                    // Toàn bộ Label, Hình ảnh bên trong sẽ tàng hình theo, không tốn 1 giọt CPU nào.
                    setGraphic(null);
                    setText(null);
                    lastItem = null;

                    // XÓA BỎ HOÀN TOÀN DÒNG NÀY (Không gọi setData(null) nữa):
                    // if (controller != null) controller.setData(null);
                } else {
                    if (this.lastItem != item) {
                        this.lastItem = item;
                        controller.setData(item);
                    }
                    // Hiện lại khung FXML sau khi đã nạp data mới
                    setGraphic(view);
                }
            }
        });

        myAuctionListView.setOnMouseClicked(event -> {
            AuctionModel selected = myAuctionListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showProductDetail(selected);
            }
        });

        loadData();
    }

    @FXML
    public void loadData() {
        String currentUser = SessionManager.userName;
        if (currentUser == null) return;

        if (loadingOverlay != null) loadingOverlay.setVisible(true);
        myAuctionListView.getItems().clear();

        // Đánh dấu thời gian bắt đầu tải
        long startTime = System.currentTimeMillis();

        CompletableFuture.runAsync(() -> {
            try {
                long apiStart = System.currentTimeMillis();

                // =========================================================
                // 1. GỌI CẢ 2 API CÙNG MỘT LÚC (Gấp đôi tốc độ)
                // =========================================================
                var balanceReq = ApiService.getAsync("/payments/" + currentUser + "/history");
                var auctionsReq = ApiService.getAsync("/auctions/my-auctions?username=" + currentUser);

                // Lệnh này bắt luồng ngầm phải chờ ĐẾN KHI CẢ 2 API ĐỀU TRẢ VỀ KẾT QUẢ
                CompletableFuture.allOf(balanceReq, auctionsReq).join();

                System.out.println("⏳ [ĐO LƯỜNG] Thời gian Server phản hồi 2 API: " + (System.currentTimeMillis() - apiStart) + "ms");

                // =========================================================
                // 2. XỬ LÝ SỐ DƯ
                // =========================================================
                var resBalance = balanceReq.get();
                if (resBalance.statusCode() == 200) {
                    com.auction.model.ApiResponse apiRes = ApiService.gson.fromJson(resBalance.body(), com.auction.model.ApiResponse.class);
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

                // =========================================================
                // 3. XỬ LÝ DANH SÁCH (Đã bỏ cơ chế Chunking vì ta đã tối ưu FXML)
                // =========================================================
                long parseStart = System.currentTimeMillis();
                var resAuctions = auctionsReq.get();
                if (resAuctions.statusCode() == 200) {
                    com.auction.model.ApiResponse apiRes = ApiService.gson.fromJson(resAuctions.body(), com.auction.model.ApiResponse.class);
                    if (apiRes.code == 1000) {
                        java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<AuctionModel>>(){}.getType();
                        List<AuctionModel> myAuctions = ApiService.gson.fromJson(apiRes.result, listType);

                        System.out.println("⏳ [ĐO LƯỜNG] Thời gian dịch JSON (" + myAuctions.size() + " items): " + (System.currentTimeMillis() - parseStart) + "ms");

                        // Đưa dữ liệu lên UI và tắt vòng xoay ngay lập tức (Không Sleep nữa)
                        long uiStart = System.currentTimeMillis();
                        Platform.runLater(() -> {
                            myAuctionListView.getItems().setAll(myAuctions);
                            if (loadingOverlay != null) loadingOverlay.setVisible(false);

                            System.out.println("⏳ [ĐO LƯỜNG] Thời gian JavaFX vẽ Danh sách: " + (System.currentTimeMillis() - uiStart) + "ms");
                            System.out.println("🚀 [HOÀN THÀNH] TỔNG THỜI GIAN: " + (System.currentTimeMillis() - startTime) + "ms\n");
                        });
                    } else {
                        Platform.runLater(() -> { if (loadingOverlay != null) loadingOverlay.setVisible(false); });
                    }
                } else {
                    Platform.runLater(() -> { if (loadingOverlay != null) loadingOverlay.setVisible(false); });
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> { if (loadingOverlay != null) loadingOverlay.setVisible(false); });
            }
        });
    }

    private void hideLoading() {
        if (loadingOverlay != null) loadingOverlay.setVisible(false);
        myAuctionListView.setOpacity(1);
    }

    private void showProductDetail(AuctionModel item) {
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

        // ===================================================================
        // TỰ ĐỘNG CẬP NHẬT QUYỀN HẠN TỪ SERVER TRƯỚC KHI CHUYỂN TRANG
        // ===================================================================

        // Hiện vòng xoay loading (nếu có) để user biết đang xử lý
        if (loadingOverlay != null) loadingOverlay.setVisible(true);

        // Gọi API lấy thông tin mới nhất của user đang đăng nhập
        ApiService.getAsync("/users/" + SessionManager.userName).thenAccept(res -> {
            Platform.runLater(() -> {
                // Tắt vòng xoay loading
                if (loadingOverlay != null) loadingOverlay.setVisible(false);

                if (res.statusCode() >= 200 && res.statusCode() < 300) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        // Giải mã thông tin user mới nhất
                        com.auction.model.UserModel currentUser = ApiService.gson.fromJson(apiRes.result, com.auction.model.UserModel.class);

                        // LÀM MỚI QUYỀN TRONG RAM MÀ KHÔNG CẦN ĐĂNG XUẤT
                        SessionManager.role = currentUser.role;
                    }
                }

                // Sau khi đã làm mới RAM, tiến hành kiểm tra quyền
                if ("SELLER".equals(SessionManager.role) || "ADMIN".equals(SessionManager.role)) {
                    // Đã là Seller -> Mở trang thêm sản phẩm
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/addauctionitem/AddProduct.fxml"));
                        Node view = loader.load();
                        StackPane contentArea = (StackPane) myAuctionListView.getScene().lookup("#contentArea");
                        if (contentArea != null) contentArea.getChildren().setAll(view);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    // Chưa là Seller -> Chặn lại và hiện thông báo
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                    alert.setTitle("Từ chối truy cập");
                    alert.setHeaderText(null);
                    alert.setContentText("Bạn chưa có quyền SELLER để đưa sản phẩm lên sàn!\nVui lòng liên hệ Admin để được nâng cấp tài khoản.");
                    com.auction.util.AlertUtils.applyStyle(alert); // Gọi CSS nếu có
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
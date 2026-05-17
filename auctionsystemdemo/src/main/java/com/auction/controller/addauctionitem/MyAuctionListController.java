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

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

public class MyAuctionListController {

    @FXML private ListView<AuctionModel> myAuctionListView;
    @FXML private Label lblBalance; // Biến hiển thị số dư

    @FXML private Label eyeIconText; // Nút Hiện/Ẩn
    private String realBalanceTextDetail = "0 VND";
    private boolean isBalanceHiddenDetail = true;

    @FXML
    public void initialize() {
        // 1. ĐỊNH DẠNG HIỂN THỊ TỪNG DÒNG
        myAuctionListView.setCellFactory(param -> new ListCell<AuctionModel>() {
            @Override
            protected void updateItem(AuctionModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/auction/MyAuctionItem.fxml"));
                        setGraphic(loader.load());

                        MyAuctionItemController controller = loader.getController();
                        controller.setData(item);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // 2. SỰ KIỆN CLICK CHUỘT VÀO SẢN PHẨM
        myAuctionListView.setOnMouseClicked(event -> {
            AuctionModel selected = myAuctionListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showProductDetail(selected);
            }
        });

        // 3. TẢI DỮ LIỆU LẦN ĐẦU
        loadData();
    }

    @FXML
    public void loadData() {
        String currentUser = SessionManager.userName;
        if (currentUser == null) return;

        // 1. Tải số dư ví (Giữ nguyên)
        ApiService.getAsync("/payments/" + currentUser + "/history").thenAccept(res -> {
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

        // ========================================================
        // 2. TẢI DANH SÁCH "SẢN PHẨM CỦA TÔI" TRỰC TIẾP TỪ DB SIÊU NHANH
        // ========================================================
        ApiService.getAsync("/auctions/my-auctions?username=" + currentUser).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        Type listType = new TypeToken<List<AuctionModel>>(){}.getType();

                        // Lúc này Backend chỉ trả về ĐÚNG sản phẩm của bạn, siêu nhẹ!
                        List<AuctionModel> myAuctions = ApiService.gson.fromJson(apiRes.result, listType);

                        ObservableList<AuctionModel> observableList = FXCollections.observableArrayList(myAuctions);
                        myAuctionListView.setItems(observableList);
                    }
                }
            });
        });
    }

    private void showProductDetail(AuctionModel item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/auction/AuctionDetail.fxml"));
            Node view = loader.load();

            AuctionDetailController controller = loader.getController();
            controller.setAuctionData(item);

            StackPane contentArea = (StackPane) myAuctionListView.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToAddProduct() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/addauctionitem/AddProduct.fxml"));
            Node view = loader.load();
            StackPane contentArea = (StackPane) myAuctionListView.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void toggleBalanceVisibility() {
        isBalanceHiddenDetail = !isBalanceHiddenDetail; // Đảo trạng thái

        if (isBalanceHiddenDetail) {
            lblBalance.setText("****** VND"); // Giấu đi
            eyeIconText.setText("Hiện");
        } else {
            lblBalance.setText(realBalanceTextDetail); // Show tiền thật
            eyeIconText.setText("Ẩn");
        }
    }
}
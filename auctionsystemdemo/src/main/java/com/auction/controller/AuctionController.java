package com.auction.controller;

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

public class AuctionController {

    @FXML private ListView<AuctionModel> auctionListView;
    @FXML private Label lblBalance;

    @FXML
    public void initialize() {
        // Cấu hình cách hiển thị từng dòng trong ListView
        auctionListView.setCellFactory(param -> new ListCell<AuctionModel>() {
            @Override
            protected void updateItem(AuctionModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/AuctionItem.fxml"));
                        setGraphic(loader.load());
                        AuctionItemController controller = loader.getController();
                        controller.setData(item);
                    } catch (IOException e) { e.printStackTrace(); }
                }
            }
        });

        // Bắt sự kiện Click vào 1 sản phẩm
        auctionListView.setOnMouseClicked(event -> {
            AuctionModel selected = auctionListView.getSelectionModel().getSelectedItem();
            if (selected != null) showDetail(selected);
        });

        loadData();
    }

    @FXML
    public void loadData() {
        // 1. Load Số dư ví
        if (SessionManager.userName != null) {
            ApiService.getAsync("/payments/" + SessionManager.userName + "/history").thenAccept(res -> {
                Platform.runLater(() -> {
                    if (res.statusCode() == 200) {
                        ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        if (apiRes.code == 1000) {
                            WalletDataResponse wallet = ApiService.gson.fromJson(apiRes.result, WalletDataResponse.class);
                            lblBalance.setText(String.format("%,.0f VND", wallet.moneyOnWallet).replace(",", "."));
                        }
                    }
                });
            });
        }

        // 2. Load Danh sách đấu giá
        ApiService.getAsync("/auctions").thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        Type listType = new TypeToken<List<AuctionModel>>(){}.getType();
                        List<AuctionModel> list = ApiService.gson.fromJson(apiRes.result, listType);
                        ObservableList<AuctionModel> observableList = FXCollections.observableArrayList(list);
                        auctionListView.setItems(observableList);
                    }
                }
            });
        });
    }

    private void showDetail(AuctionModel item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/AuctionDetail.fxml"));
            Node detailView = loader.load();
            AuctionDetailController controller = loader.getController();
            controller.setAuctionData(item);

            StackPane contentArea = (StackPane) auctionListView.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(detailView);
        } catch (IOException e) { e.printStackTrace(); }
    }
}
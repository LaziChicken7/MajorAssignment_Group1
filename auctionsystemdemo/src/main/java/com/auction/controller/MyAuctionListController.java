package com.auction.controller;

import com.auction.model.ApiResponse;
import com.auction.model.AuctionModel;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

public class MyAuctionListController {

    // Đã đổi toàn bộ sang AuctionModel
    @FXML private ListView<AuctionModel> myAuctionListView;

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
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/MyAuctionItem.fxml"));
                        setGraphic(loader.load());

                        // Lấy controller của dòng đó để nạp dữ liệu
                        MyAuctionItemController controller = loader.getController();
                        controller.setData(item);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // 2. SỰ KIỆN CLICK CHUỘT
        myAuctionListView.setOnMouseClicked(event -> {
            AuctionModel selected = myAuctionListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showProductDetail(selected);
            }
        });

        // 3. GỌI API LẤY DỮ LIỆU
        loadDataFromApi();
    }

    private void loadDataFromApi() {
        String currentUser = SessionManager.userName;
        if (currentUser == null) return;

        // Gọi API lấy TẤT CẢ sản phẩm
        ApiService.getAsync("/auctions").thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        Type listType = new TypeToken<List<AuctionModel>>(){}.getType();
                        List<AuctionModel> allAuctions = ApiService.gson.fromJson(apiRes.result, listType);

                        // LỌC RA NHỮNG SẢN PHẨM MÀ SELLER CHÍNH LÀ MÌNH
                        List<AuctionModel> myAuctions = allAuctions.stream()
                                .filter(a -> a.seller != null && currentUser.equals(a.seller.userName))
                                .collect(Collectors.toList());

                        ObservableList<AuctionModel> observableList = FXCollections.observableArrayList(myAuctions);
                        myAuctionListView.setItems(observableList);
                    }
                }
            });
        });
    }

    // Đã đổi tham số truyền vào thành AuctionModel (HẾT LỖI Ở ĐÂY)
    private void showProductDetail(AuctionModel item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/AuctionDetail.fxml"));
            Node view = loader.load();

            // Truyền dữ liệu sp sang trang chi tiết
            AuctionDetailController controller = loader.getController();
            controller.setAuctionData(item);

            // Nạp vào vùng contentArea (Giữ Sidebar)
            StackPane contentArea = (StackPane) myAuctionListView.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToAddProduct() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/AddProduct.fxml"));
            Node view = loader.load();
            StackPane contentArea = (StackPane) myAuctionListView.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
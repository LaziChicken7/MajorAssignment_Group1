package com.auction.controller;

import com.auction.model.AuctionItem;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class MyAuctionListController {
    @FXML private ListView<AuctionItem> myAuctionListView;

    @FXML
    public void initialize() {
        // 1. KẾT NỐI DỮ LIỆU
        myAuctionListView.setItems(com.auction.controller.AuctionService.getMyAuctions());

        // 2. THÊM SỰ KIỆN CLICK CHUỘT (Để vào xem chi tiết) - BẠN ĐANG THIẾU CÁI NÀY
        myAuctionListView.setOnMouseClicked(event -> {
            AuctionItem selected = myAuctionListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                System.out.println(">>> Đang mở chi tiết sp: " + selected.getName());
                showProductDetail(selected);
            }
        });

        // 3. ĐỊNH DẠNG HIỂN THỊ TỪNG DÒNG (CellFactory)
        myAuctionListView.setCellFactory(param -> new ListCell<AuctionItem>() {
            @Override
            protected void updateItem(AuctionItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/MyAuctionItem.fxml"));
                        if (loader.getLocation() == null) {
                            System.err.println(">>> LỖI: Không thấy file MyAuctionItem.fxml!");
                            return;
                        }
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
    }

    // HÀM MỚI: CHUYỂN SANG TRANG CHI TIẾT
    private void showProductDetail(AuctionItem item) {
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
            System.err.println(">>> LỖI LOAD TRANG CHI TIẾT: " + e.getMessage());
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
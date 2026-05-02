package com.auction.controller;

import com.auction.model.AuctionItem;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class AuctionController {

    @FXML
    private ListView<AuctionItem> auctionListView;

    @FXML
    public void initialize() {
        // Kết nối ListView với kho dữ liệu AuctionService
        auctionListView.setItems(com.auction.controller.AuctionService.getAllAuctions());

        // Lắng nghe click chuột để vào xem chi tiết
        auctionListView.setOnMouseClicked(event -> {
            AuctionItem selected = auctionListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showDetail(selected);
            }
        });

        // Đổ giao diện đẹp cho từng dòng
        auctionListView.setCellFactory(param -> new ListCell<AuctionItem>() {
            @Override
            protected void updateItem(AuctionItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/AuctionItem.fxml"));
                        setGraphic(loader.load());
                        AuctionItemController controller = loader.getController();
                        controller.setData(item);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void showDetail(AuctionItem item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/AuctionDetail.fxml"));
            Node detailView = loader.load();

            AuctionDetailController controller = loader.getController();
            controller.setAuctionData(item);

            // Thay thế vùng nội dung chính (giữ Sidebar)
            StackPane contentArea = (StackPane) auctionListView.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(detailView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
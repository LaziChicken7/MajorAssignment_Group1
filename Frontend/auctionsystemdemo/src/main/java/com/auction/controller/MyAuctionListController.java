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
        // 1. KẾT NỐI DANH SÁCH VỚI KHO DỮ LIỆU CHUNG (Cực kỳ quan trọng)
        myAuctionListView.setItems(AuctionService.getAllAuctions());

        // 2. Định dạng hiển thị từng dòng (CellFactory)
        myAuctionListView.setCellFactory(param -> new ListCell<AuctionItem>() {
            @Override
            protected void updateItem(AuctionItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    // Trong MyAuctionListController.java
                    try {
                        // Đảm bảo đường dẫn này khớp 100% với cấu trúc thư mục của bạn
                        // Lưu ý: Java phân biệt chữ HOA và chữ thường (MyAuctionItem != myauctionitem)
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/MyAuctionItem.fxml"));

                        // Thêm dòng kiểm tra này để debug dễ hơn
                        if (loader.getLocation() == null) {
                            System.err.println(">>> LỖI: Không tìm thấy file MyAuctionItem.fxml! Kiểm tra lại thư mục resources.");
                            return;
                        }

                        setGraphic(loader.load());

                        MyAuctionItemController controller = loader.getController();
                        controller.setData(item);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @FXML
    private void goToAddProduct() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/AddProduct.fxml"));
            Node view = loader.load();

            // Tìm vùng contentArea (phải có ID này trong Main.fxml)
            StackPane contentArea = (StackPane) myAuctionListView.getScene().lookup("#contentArea");

            if (contentArea != null) {
                contentArea.getChildren().setAll(view); // Thay thế toàn bộ nội dung cũ bằng trang AddProduct
            }
        } catch (Exception e) {
            System.err.println("LỖI KHI MỞ TRANG THÊM: " + e.getMessage());
            e.printStackTrace(); // Xem lỗi đỏ ở console để biết sai ở đâu
        }
    }
}
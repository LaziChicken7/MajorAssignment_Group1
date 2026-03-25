package com.auction.controller;

import com.auction.App;
import com.auction.model.AuctionItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.io.IOException;

public class HomeController {
    @FXML private Label lblWelcome;

    // Khai báo các thành phần của Bảng
    @FXML private TableView<AuctionItem> auctionTable;
    @FXML private TableColumn<AuctionItem, String> colId;
    @FXML private TableColumn<AuctionItem, String> colName;
    @FXML private TableColumn<AuctionItem, Double> colPrice;
    @FXML private TableColumn<AuctionItem, String> colEndTime;
    @FXML private TableColumn<AuctionItem, String> colStatus;

    @FXML
    public void initialize() {
        lblWelcome.setText("Xin chào, Admin!");

        // 1. Cấu hình các cột sẽ lấy dữ liệu từ thuộc tính nào của AuctionItem
        // Tên trong ngoặc kép phải khớp CHÍNH XÁC với tên biến trong class AuctionItem
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // 2. Tạo danh sách dữ liệu giả (Mock Data)
        ObservableList<AuctionItem> mockData = FXCollections.observableArrayList(
                new AuctionItem("SP001", "Laptop Dell XPS 15", 1250.0, "2026-04-10 15:00", "OPEN"),
                new AuctionItem("SP002", "iPhone 15 Pro Max", 999.0, "2026-03-30 10:30", "RUNNING"),
                new AuctionItem("SP003", "Tranh sơn dầu thế kỷ 19", 5000.0, "2026-05-01 09:00", "OPEN"),
                new AuctionItem("SP004", "Đồng hồ Rolex Submariner", 15500.0, "2026-03-25 18:00", "FINISHED"),
                new AuctionItem("SP005", "Mô hình Gundam Limited", 320.5, "2026-03-28 20:00", "RUNNING")
        );

        // 3. Đưa dữ liệu vào bảng
        auctionTable.setItems(mockData);
    }

    @FXML
    private void handleLogout() throws IOException {
        // Quay lại màn hình Login
        App.setRoot("view/Login");
    }
}
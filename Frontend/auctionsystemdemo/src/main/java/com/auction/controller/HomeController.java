package com.auction.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

public class HomeController {

    @FXML
    private LineChart<String, Number> chartAuction;

    @FXML
    public void initialize() {
        // Hàm này chạy ngay khi Dashboard vừa load xong
        // Bạn có thể fetch dữ liệu biểu đồ hoặc danh sách SP nổi bật ở đây
        System.out.println("Load dữ liệu trang Dashboard...");
        
        // Thêm dữ liệu cho chart
        loadChartData();
    }

    private void loadChartData() {
        if (chartAuction == null) return;
        
        // Tạo 3 series dữ liệu cho các sản phẩm đấu giá nổi bật
        XYChart.Series<String, Number> series1 = new XYChart.Series<>();
        series1.setName("Auction name 1");
        series1.getData().addAll(
            new XYChart.Data<>("2021", 12),
            new XYChart.Data<>("2022", 15),
            new XYChart.Data<>("2023", 18),
            new XYChart.Data<>("2024", 22),
            new XYChart.Data<>("2025", 25)
        );

        XYChart.Series<String, Number> series2 = new XYChart.Series<>();
        series2.setName("Auction name 2");
        series2.getData().addAll(
            new XYChart.Data<>("2021", 10),
            new XYChart.Data<>("2022", 13),
            new XYChart.Data<>("2023", 16),
            new XYChart.Data<>("2024", 20),
            new XYChart.Data<>("2025", 24)
        );

        XYChart.Series<String, Number> series3 = new XYChart.Series<>();
        series3.setName("Auction name 3");
        series3.getData().addAll(
            new XYChart.Data<>("2021", 9),
            new XYChart.Data<>("2022", 12),
            new XYChart.Data<>("2023", 14),
            new XYChart.Data<>("2024", 17),
            new XYChart.Data<>("2025", 20)
        );

        chartAuction.getData().addAll(series1, series2, series3);
    }

    @FXML
    public void handleViewMoreAuctions(ActionEvent event) {
        System.out.println("Xem thêm sản phẩm nổi bật...");
    }

    @FXML
    public void handleViewMoreNotifications(ActionEvent event) {
        System.out.println("Xem thêm thông báo...");
    }
}
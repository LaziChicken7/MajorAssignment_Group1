package com.auction.controller.auction;

import com.auction.model.ApiResponse;
import com.auction.model.AuctionModel;
import com.auction.util.ApiService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.List;

public class AuctionChartController {

    @FXML private LineChart<String, Number> priceChart;
    @FXML private Label lblChartTitle;

    private Timeline chartTimeline;
    private AuctionModel currentItem;
    private XYChart.Series<String, Number> priceSeries;

    public void setAuctionData(AuctionModel item) {
        this.currentItem = item;
        // Cập nhật tên sản phẩm lên luôn chỗ nút Back
        if (lblChartTitle != null) {
            lblChartTitle.setText("Biểu đồ đấu giá trực tiếp - " + item.bidProduct.name);
        }
        initChart(item.id);
    }

    private void initChart(String auctionId) {
        // Khởi tạo Series
        priceChart.getData().clear();
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Lịch sử giá");
        priceChart.getData().add(priceSeries);

        // Quét API 2 giây / lần
        chartTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            ApiService.getAsync("/auctions/" + auctionId + "/price-chart").thenAccept(res -> {
                Platform.runLater(() -> {
                    if (res.statusCode() == 200) {
                        ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        if (apiRes.code == 1000) {
                            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<AuctionModel.BidTransactionModel>>(){}.getType();
                            List<AuctionModel.BidTransactionModel> txs = ApiService.gson.fromJson(apiRes.result, listType);

                            if (txs == null || txs.isEmpty()) return;

                            // So sánh: Nếu có giao dịch mới thì mới vẽ thêm điểm
                            if (txs.size() > priceSeries.getData().size()) {
                                for (int i = priceSeries.getData().size(); i < txs.size(); i++) {
                                    AuctionModel.BidTransactionModel tx = txs.get(i);
                                    String timeLabel = tx.bidTimestamp.contains("T") ? tx.bidTimestamp.substring(11, 19) : tx.bidTimestamp;

                                    // Thêm điểm nối mới vào biểu đồ
                                    priceSeries.getData().add(new XYChart.Data<>(timeLabel, tx.bidAmount));
                                }
                            }
                        }
                    }
                });
            });
        }));
        chartTimeline.setCycleCount(Timeline.INDEFINITE);
        chartTimeline.play();
    }

    // =====================================
    // NÚT BACK (QUAY LẠI TRANG CHI TIẾT)
    // =====================================
    @FXML
    private void goBack() {
        if (chartTimeline != null) chartTimeline.stop(); // Tắt biểu đồ

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/auction/AuctionDetail.fxml"));
            Node view = loader.load();

            // Ném ngược cục Data về lại trang Detail để nó Render tiếp
            AuctionDetailController controller = loader.getController();
            controller.setAuctionData(currentItem);

            StackPane contentArea = (StackPane) priceChart.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
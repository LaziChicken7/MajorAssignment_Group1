package com.auction.controller.auction;

import com.auction.model.ApiResponse;
import com.auction.model.AuctionModel;
import com.auction.util.ApiService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AuctionChartController {

    @FXML private VBox chartContainer;
    @FXML private LineChart<Number, Number> priceChart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private Label lblChartTitle;

    private Timeline chartTimeline;
    private AuctionModel currentItem;
    private XYChart.Series<Number, Number> priceSeries;

    // Biến dùng cho Kéo thả (Pan Data)
    private double dragStartX, dragStartY;
    private double xLowerStart, xUpperStart;
    private double yLowerStart, yUpperStart;

    // GIỚI HẠN BIỂU ĐỒ (Chặn quá khứ và giá âm)
    private long minX;
    private final double minY = 0.0;

    public void setAuctionData(AuctionModel item) {
        this.currentItem = item;
        if (lblChartTitle != null) {
            lblChartTitle.setText("Biểu đồ đấu giá trực tiếp - " + item.bidProduct.name);
        }
        initChart(item.id);
    }

    private void initChart(String auctionId) {
        priceChart.setAnimated(false);
        priceChart.getData().clear();
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Lịch sử giá");
        priceChart.getData().add(priceSeries);

        // =======================================================
        // 1. THIẾT LẬP GIỚI HẠN THỜI GIAN BẮT ĐẦU (minX)
        // =======================================================
        String startStr = currentItem.startTime.contains("T") ? currentItem.startTime : currentItem.startTime.replace(" ", "T");
        LocalDateTime startObj = LocalDateTime.parse(startStr);
        minX = startObj.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        // =======================================================
        // 2. DỊCH TRỤC X (MILI-GIÂY) THÀNH CHỮ HH:mm:ss
        // =======================================================
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        xAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(object.longValue()), ZoneId.systemDefault());
                return time.format(formatter);
            }
            @Override
            public Number fromString(String string) { return 0; }
        });

        // =======================================================
        // 3. FETCH DỮ LIỆU TỪ API
        // =======================================================
        chartTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            ApiService.getAsync("/auctions/" + auctionId + "/price-chart").thenAccept(res -> {
                Platform.runLater(() -> {
                    if (res.statusCode() == 200) {
                        ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        if (apiRes.code == 1000) {
                            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<AuctionModel.BidTransactionModel>>(){}.getType();
                            List<AuctionModel.BidTransactionModel> txs = ApiService.gson.fromJson(apiRes.result, listType);

                            if (txs == null || txs.isEmpty()) return;

                            if (txs.size() > priceSeries.getData().size()) {
                                for (int i = priceSeries.getData().size(); i < txs.size(); i++) {
                                    AuctionModel.BidTransactionModel tx = txs.get(i);

                                    // Chuyển chuỗi thời gian thành Mili-giây (Long/Number)
                                    String timeStr = tx.bidTimestamp.contains("T") ? tx.bidTimestamp : tx.bidTimestamp.replace(" ", "T");
                                    LocalDateTime timeObj = LocalDateTime.parse(timeStr);
                                    long epochMillis = timeObj.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

                                    // Chỉ cho phép vẽ các điểm nằm sau thời gian bắt đầu
                                    if (epochMillis >= minX && tx.bidAmount >= minY) {
                                        priceSeries.getData().add(new XYChart.Data<>(epochMillis, tx.bidAmount));
                                    }
                                }
                            }
                        }
                    }
                });
            });
        }));
        chartTimeline.setCycleCount(Timeline.INDEFINITE);
        chartTimeline.play();

        enableZoomAndPan();
    }

    // =====================================
    // LOGIC ZOOM & PAN CÓ KẸP CHẶN GIỚI HẠN
    // =====================================
    private void enableZoomAndPan() {
        // 1. ZOOM (Lăn chuột)
        priceChart.setOnScroll(event -> {
            event.consume();
            if (event.getDeltaY() == 0) return;

            xAxis.setAutoRanging(false);
            yAxis.setAutoRanging(false);

            double zoomFactor = (event.getDeltaY() > 0) ? 0.8 : 1.2;

            // Tính toán giới hạn mới cho 2 trục
            double xRange = xAxis.getUpperBound() - xAxis.getLowerBound();
            double xCenter = (xAxis.getUpperBound() + xAxis.getLowerBound()) / 2.0;
            double newXLower = xCenter - (xRange * zoomFactor) / 2.0;
            double newXUpper = xCenter + (xRange * zoomFactor) / 2.0;

            double yRange = yAxis.getUpperBound() - yAxis.getLowerBound();
            double yCenter = (yAxis.getUpperBound() + yAxis.getLowerBound()) / 2.0;
            double newYLower = yCenter - (yRange * zoomFactor) / 2.0;
            double newYUpper = yCenter + (yRange * zoomFactor) / 2.0;

            // KẸP CHẶN ZOOM (Chống lùi về trước startTime)
            if (newXLower < minX) {
                newXLower = minX;
                newXUpper = newXLower + (xRange * zoomFactor); // Đẩy Upper lên để không bị bóp méo khung zoom
            }

            // KẸP CHẶN ZOOM (Chống lùi về giá âm)
            if (newYLower < minY) {
                newYLower = minY;
                newYUpper = newYLower + (yRange * zoomFactor);
            }

            xAxis.setLowerBound(newXLower);
            xAxis.setUpperBound(newXUpper);
            yAxis.setLowerBound(newYLower);
            yAxis.setUpperBound(newYUpper);
        });

        // 2. PAN (Kéo thả biểu đồ)
        priceChart.setOnMousePressed(event -> {
            dragStartX = event.getX();
            dragStartY = event.getY();
            xLowerStart = xAxis.getLowerBound();
            xUpperStart = xAxis.getUpperBound();
            yLowerStart = yAxis.getLowerBound();
            yUpperStart = yAxis.getUpperBound();
            priceChart.setCursor(Cursor.CLOSED_HAND);
        });

        priceChart.setOnMouseDragged(event -> {
            xAxis.setAutoRanging(false);
            yAxis.setAutoRanging(false);

            double deltaX = event.getX() - dragStartX;
            double deltaY = event.getY() - dragStartY;

            double dataDeltaX = deltaX / xAxis.getScale();
            double dataDeltaY = deltaY / yAxis.getScale();

            // Tính khoảng dịch chuyển (Đã đảo chiều Y để chuột đi đúng hướng)
            double newXLower = xLowerStart - dataDeltaX;
            double newXUpper = xUpperStart - dataDeltaX;
            double newYLower = yLowerStart - dataDeltaY;
            double newYUpper = yUpperStart - dataDeltaY;

            // KẸP CHẶN KÉO THẢ (Trục X)
            if (newXLower < minX) {
                double diff = minX - newXLower;
                newXLower += diff;
                newXUpper += diff;
            }

            // KẸP CHẶN KÉO THẢ (Trục Y)
            if (newYLower < minY) {
                double diff = minY - newYLower;
                newYLower += diff;
                newYUpper += diff;
            }

            xAxis.setLowerBound(newXLower);
            xAxis.setUpperBound(newXUpper);
            yAxis.setLowerBound(newYLower);
            yAxis.setUpperBound(newYUpper);
        });

        priceChart.setOnMouseReleased(event -> {
            priceChart.setCursor(Cursor.DEFAULT);
        });
    }

    // =====================================
    // NÚT KHÔI PHỤC (RESET ZOOM)
    // =====================================
    @FXML
    private void resetZoom() {
        xAxis.setAutoRanging(true);
        yAxis.setAutoRanging(true);
    }

    // =====================================
    // NÚT QUAY LẠI TRANG CHI TIẾT
    // =====================================
    @FXML
    private void goBack() {
        if (chartTimeline != null) chartTimeline.stop();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/auction/AuctionDetail.fxml"));
            Node view = loader.load();

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
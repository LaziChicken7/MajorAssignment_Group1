package com.auction.controller.auction;

import com.auction.model.ApiResponse;
import com.auction.model.AuctionModel;
import com.auction.util.ApiService;
import com.auction.util.GlobalWebSocketManager;
import javafx.animation.PauseTransition;
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

    private AuctionModel currentItem;
    private XYChart.Series<Number, Number> priceSeries;

    // Biến lưu trạng thái vòng lặp
    private int lastProcessedIndex = 0;
    private long lastEpochMillis = 0;

    // Biến lưu MỐC THỜI GIAN GỐC để chuẩn hóa trục X (Tránh lỗi tràn số của JavaFX)
    private long baseTime;

    // Biến dùng cho Kéo thả
    private double dragStartX, dragStartY;
    private double xLowerStart, xUpperStart;
    private double yLowerStart, yUpperStart;
    private final double minY = 0.0;

    // BỘ ĐẾM CHỜ CHỐNG SPAM WEBSOCKET
    private PauseTransition wsDebouncer = new PauseTransition(Duration.millis(400));

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

        // 1. LẤY MỐC BASE-TIME (Thời gian bắt đầu)
        String startStr = currentItem.startTime.contains("T") ? currentItem.startTime : currentItem.startTime.replace(" ", "T");
        LocalDateTime startObj = LocalDateTime.parse(startStr);
        baseTime = startObj.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        // 2. FORMAT TRỤC X: Cộng ngược baseTime vào độ lệch để in ra giờ thực tế
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        xAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                long realEpoch = baseTime + object.longValue();
                LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(realEpoch), ZoneId.systemDefault());
                return time.format(formatter);
            }
            @Override
            public Number fromString(String string) { return 0; }
        });

        // ==============================================================
        // 3. TẢI DỮ LIỆU LẦN ĐẦU VÀ KÍCH HOẠT WEBSOCKET LẮNG NGHE REAL-TIME
        // ==============================================================
        fetchChartDataFromServer(auctionId); // Lần tải mồi đầu tiên

        GlobalWebSocketManager.listenToAuction(auctionId, () -> {
            Platform.runLater(() -> {
                wsDebouncer.setOnFinished(e -> {
                    fetchChartDataFromServer(auctionId);
                });
                wsDebouncer.playFromStart();
            });
        });

        enableZoomAndPan();
    }

    // Hàm gọi API lấy dữ liệu và vẽ lên biểu đồ
    private void fetchChartDataFromServer(String auctionId) {
        ApiService.getAsync("/auctions/" + auctionId + "/price-chart").thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<AuctionModel.BidTransactionModel>>(){}.getType();
                        List<AuctionModel.BidTransactionModel> txs = ApiService.gson.fromJson(apiRes.result, listType);

                        if (txs == null || txs.isEmpty()) return;

                        // Sắp xếp dữ liệu theo thời gian thực (Tránh chart bị đứt gãy)
                        txs.sort((t1, t2) -> {
                            int timeCompare = t1.bidTimestamp.compareTo(t2.bidTimestamp);
                            if (timeCompare == 0) return Double.compare(t1.bidAmount, t2.bidAmount);
                            return timeCompare;
                        });

                        // NẾU NGƯỜI CHƠI BID TRƯỚC CẢ GIỜ BẮT ĐẦU -> Kéo lùi mốc baseTime lại để không bị chặn
                        String firstStr = txs.get(0).bidTimestamp.contains("T") ? txs.get(0).bidTimestamp : txs.get(0).bidTimestamp.replace(" ", "T");
                        long firstBidEpoch = LocalDateTime.parse(firstStr).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                        if (firstBidEpoch < baseTime && lastProcessedIndex == 0) {
                            baseTime = firstBidEpoch;
                        }

                        // Vòng lặp Add Data (Chỉ vẽ tiếp những điểm mới)
                        if (txs.size() > lastProcessedIndex) {
                            for (int i = lastProcessedIndex; i < txs.size(); i++) {
                                AuctionModel.BidTransactionModel tx = txs.get(i);

                                String timeStr = tx.bidTimestamp.contains("T") ? tx.bidTimestamp : tx.bidTimestamp.replace(" ", "T");
                                LocalDateTime timeObj = LocalDateTime.parse(timeStr);
                                long epochMillis = timeObj.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

                                // Xử lý nhiều người bid trùng 1 giây: Cộng dồn 10ms để tách rẽ điểm
                                if (epochMillis <= lastEpochMillis) {
                                    epochMillis = lastEpochMillis + 10;
                                }
                                lastEpochMillis = epochMillis;

                                // Thuật toán Chuẩn hoá X
                                long xValue = epochMillis - baseTime;

                                if (xValue >= 0 && tx.bidAmount >= minY) {
                                    priceSeries.getData().add(new XYChart.Data<>(xValue, tx.bidAmount));
                                }
                            }
                            lastProcessedIndex = txs.size(); // Ghi nhớ tổng số điểm đã vẽ
                        }
                    }
                }
            });
        });
    }

    private void enableZoomAndPan() {
        priceChart.setOnScroll(event -> {
            event.consume();
            if (event.getDeltaY() == 0) return;

            xAxis.setAutoRanging(false);
            yAxis.setAutoRanging(false);

            double zoomFactor = (event.getDeltaY() > 0) ? 0.8 : 1.2;

            double xRange = xAxis.getUpperBound() - xAxis.getLowerBound();
            double xCenter = (xAxis.getUpperBound() + xAxis.getLowerBound()) / 2.0;
            double newXLower = xCenter - (xRange * zoomFactor) / 2.0;
            double newXUpper = xCenter + (xRange * zoomFactor) / 2.0;

            double yRange = yAxis.getUpperBound() - yAxis.getLowerBound();
            double yCenter = (yAxis.getUpperBound() + yAxis.getLowerBound()) / 2.0;
            double newYLower = yCenter - (yRange * zoomFactor) / 2.0;
            double newYUpper = yCenter + (yRange * zoomFactor) / 2.0;

            // Kẹp chặn (bây giờ mức giới hạn nhỏ nhất của trục X là 0)
            if (newXLower < 0) {
                newXLower = 0;
                newXUpper = newXLower + (xRange * zoomFactor);
            }

            if (newYLower < minY) {
                newYLower = minY;
                newYUpper = newYLower + (yRange * zoomFactor);
            }

            xAxis.setLowerBound(newXLower);
            xAxis.setUpperBound(newXUpper);
            yAxis.setLowerBound(newYLower);
            yAxis.setUpperBound(newYUpper);
        });

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

            double newXLower = xLowerStart - dataDeltaX;
            double newXUpper = xUpperStart - dataDeltaX;
            double newYLower = yLowerStart - dataDeltaY;
            double newYUpper = yUpperStart - dataDeltaY;

            if (newXLower < 0) {
                double diff = 0 - newXLower;
                newXLower += diff;
                newXUpper += diff;
            }

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

    @FXML
    private void resetZoom() {
        xAxis.setAutoRanging(true);
        yAxis.setAutoRanging(true);
    }

    @FXML
    private void goBack() {
        // =======================================================
        // ĐÓNG WEBSOCKET KHI QUAY LẠI ĐỂ TIẾT KIỆM TÀI NGUYÊN MẠNG
        // =======================================================
        GlobalWebSocketManager.stopListeningAuction();

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
package com.auction.controller;

import com.auction.model.ApiResponse;
import com.auction.model.AuctionModel;
import com.auction.model.NotificationModel;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import com.google.gson.reflect.TypeToken;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HomeController {

    @FXML private VBox vboxFeaturedAuctions;
    @FXML private VBox vboxNotifications;
    @FXML private Label lblNotifCount;
    @FXML private LineChart<String, Number> chartAuction;

    private Timeline masterTimeline; // Đồng hồ tổng quản lý thời gian trên Dashboard

    @FXML
    public void initialize() {
        loadAuctionsAndChart();
        loadNotifications();
    }

    private void loadAuctionsAndChart() {
        ApiService.getAsync("/auctions").thenAccept(res -> {
            Platform.runLater(() -> {
                try {
                    if (res.statusCode() == 200) {
                        ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        if (apiRes.code == 1000) {
                            Type listType = new TypeToken<List<AuctionModel>>(){}.getType();
                            List<AuctionModel> allAuctions = ApiService.gson.fromJson(apiRes.result, listType);

                            // Lấy 4 sản phẩm đang RUNNING hoặc OPEN, sắp xếp theo giá
                            List<AuctionModel> topAuctions = allAuctions.stream()
                                    .filter(a -> "RUNNING".equals(a.status) || "OPEN".equals(a.status))
                                    .sorted((a1, a2) -> Double.compare(a2.highestBid, a1.highestBid))
                                    .limit(4)
                                    .collect(Collectors.toList());

                            vboxFeaturedAuctions.getChildren().clear();
                            chartAuction.getData().clear();

                            // Xóa đồng hồ cũ nếu có
                            if (masterTimeline != null) masterTimeline.stop();
                            List<Runnable> timerTasks = new ArrayList<>();

                            XYChart.Series<String, Number> seriesStart = new XYChart.Series<>();
                            seriesStart.setName("Giá khởi điểm");
                            XYChart.Series<String, Number> seriesCurrent = new XYChart.Series<>();
                            seriesCurrent.setName("Giá hiện tại");

                            // LUÔN LUÔN DUYỆT 4 LẦN ĐỂ CỐ ĐỊNH CHIỀU CAO UI
                            for (int i = 0; i < 4; i++) {
                                HBox row = new HBox(20);
                                row.setAlignment(Pos.CENTER_LEFT);

                                if (i < topAuctions.size()) {
                                    // CÓ DỮ LIỆU -> Vẽ dòng sản phẩm thực tế
                                    AuctionModel a = topAuctions.get(i);
                                    row.setStyle("-fx-background-color: #F8F9FB; -fx-background-radius: 15; -fx-padding: 10 25; -fx-min-height: 55;");

                                    String shortId = a.bidProduct != null && a.bidProduct.id.length() >= 4
                                            ? a.bidProduct.id.substring(0, 4).toUpperCase() : "N/A";
                                    String name = a.bidProduct != null ? a.bidProduct.name : "Sản phẩm ẩn";
                                    double startPrice = a.bidProduct != null ? a.bidProduct.startPrice : 0;

                                    Label lblId = new Label("SP" + shortId);
                                    lblId.setPrefWidth(80);
                                    lblId.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");

                                    Label lblName = new Label(name);
                                    lblName.setStyle("-fx-font-size: 16px; -fx-text-fill: #333;");

                                    Region spacer = new Region();
                                    HBox.setHgrow(spacer, Priority.ALWAYS);

                                    Label lblPrice = new Label(String.format("%,.0f VND", a.highestBid).replace(",", "."));
                                    lblPrice.setStyle("-fx-background-color: #0A439D; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 6 20; -fx-font-weight: bold; -fx-font-size: 15px;");

                                    // XỬ LÝ TRẠNG THÁI & ĐỒNG HỒ
                                    String baseColor = "OPEN".equals(a.status) ? "#3498db" : "#f39c12"; // Xanh(OPEN) hoặc Cam(RUNNING)
                                    String prefixText = "OPEN".equals(a.status) ? "Sắp bắt đầu sau:" : "Thời gian còn lại:";
                                    String targetTimeStr = "OPEN".equals(a.status) ? a.startTime : a.endTime;

                                    Label lblTimePrefix = new Label(prefixText);
                                    lblTimePrefix.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");

                                    Label lblTime = new Label("00:00:00");
                                    lblTime.setStyle("-fx-background-color: " + baseColor + "; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 6 20; -fx-font-weight: bold; -fx-font-size: 15px;");

                                    if (targetTimeStr != null) {
                                        String timeStr = targetTimeStr.contains("T") ? targetTimeStr : targetTimeStr.replace(" ", "T");
                                        LocalDateTime targetTime = LocalDateTime.parse(timeStr);

                                        // Thêm tác vụ đếm ngược vào List
                                        timerTasks.add(() -> {
                                            LocalDateTime now = LocalDateTime.now();
                                            if (now.isAfter(targetTime)) {
                                                lblTime.setText("00:00:00");
                                                lblTime.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 6 20; -fx-font-weight: bold; -fx-font-size: 15px;");
                                            } else {
                                                java.time.Duration duration = java.time.Duration.between(now, targetTime);
                                                long hours = duration.toHours();
                                                long minutes = duration.toMinutesPart();
                                                long seconds = duration.toSecondsPart();
                                                lblTime.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));

                                                if ("RUNNING".equals(a.status) && hours == 0 && minutes < 10) {
                                                    lblTime.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 6 20; -fx-font-weight: bold; -fx-font-size: 15px;"); // Nháy đỏ
                                                }
                                            }
                                        });
                                    }

                                    row.getChildren().addAll(lblId, lblName, spacer, lblPrice, lblTimePrefix, lblTime);

                                    // Đẩy vào biểu đồ
                                    seriesStart.getData().add(new XYChart.Data<>("SP" + shortId, startPrice));
                                    seriesCurrent.getData().add(new XYChart.Data<>("SP" + shortId, a.highestBid));

                                } else {
                                    // KHÔNG CÓ DỮ LIỆU -> Bỏ trắng hoàn toàn, chỉ giữ lại chiều cao
                                    row.setStyle("-fx-background-color: transparent; -fx-min-height: 55;");
                                }

                                vboxFeaturedAuctions.getChildren().add(row);
                            }

                            // Chạy đồng hồ nếu có task
                            if (!timerTasks.isEmpty()) {
                                masterTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                                    for (Runnable task : timerTasks) task.run();
                                }));
                                masterTimeline.setCycleCount(Timeline.INDEFINITE);
                                masterTimeline.play();
                            }

                            if (!topAuctions.isEmpty()) {
                                chartAuction.getData().addAll(seriesStart, seriesCurrent);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Lỗi parse JSON ở Dashboard: " + e.getMessage());
                }
            });
        });
    }

    private void loadNotifications() {
        if (SessionManager.userName == null) return;
        ApiService.getAsync("/notifications/" + SessionManager.userName).thenAccept(res -> {
            Platform.runLater(() -> {
                try {
                    if (res.statusCode() == 200) {
                        ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        if (apiRes.code == 1000) {
                            Type listType = new TypeToken<List<NotificationModel>>(){}.getType();
                            List<NotificationModel> allNotifs = ApiService.gson.fromJson(apiRes.result, listType);

                            long unreadCount = allNotifs.stream().filter(n -> !n.isRead).count();
                            lblNotifCount.setText(unreadCount > 9 ? "9+" : String.valueOf(unreadCount));
                            lblNotifCount.setVisible(unreadCount > 0);

                            vboxNotifications.getChildren().clear();

                            if (allNotifs.isEmpty()) {
                                Label lbl = new Label("Không có thông báo nào mới.");
                                lbl.setStyle("-fx-font-style: italic; -fx-text-fill: #888; -fx-font-size: 15px;");
                                vboxNotifications.getChildren().add(lbl);
                                return;
                            }

                            int limit = Math.min(allNotifs.size(), 2);
                            for (int i = 0; i < limit; i++) {
                                NotificationModel n = allNotifs.get(i);
                                HBox row = new HBox(15);
                                row.setAlignment(Pos.CENTER_LEFT);
                                row.setStyle("-fx-background-color: #F8F9FB; -fx-background-radius: 10; -fx-padding: 15;");

                                VBox textVBox = new VBox(5);
                                Label lblTitle = new Label(n.title);
                                lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #333;");

                                Label lblDesc = new Label(n.description);
                                lblDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #666;");
                                lblDesc.setWrapText(true);
                                lblDesc.setMaxWidth(220);

                                textVBox.getChildren().addAll(lblTitle, lblDesc);

                                Region spacer = new Region();
                                HBox.setHgrow(spacer, Priority.ALWAYS);

                                Button btnView = new Button("Chi tiết");
                                btnView.setStyle("-fx-background-color: #0A439D; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-weight: bold;");
                                btnView.setOnAction(e -> handleViewMoreNotifications(e));

                                row.getChildren().addAll(textVBox, spacer, btnView);
                                vboxNotifications.getChildren().add(row);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Lỗi load Thông báo ở Dashboard.");
                }
            });
        });
    }

    private void switchView(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            Node source = (Node) event.getSource();
            Pane contentArea = (Pane) source.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToAuctionList(ActionEvent event) {
        switchView(event, "/com/auction/view/AuctionList.fxml");
    }

    @FXML
    public void handleViewMoreNotifications(ActionEvent event) {
        switchView(event, "/com/auction/view/NotificationList.fxml");
    }
}
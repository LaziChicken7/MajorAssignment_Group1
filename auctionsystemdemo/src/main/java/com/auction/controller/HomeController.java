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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
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
    @FXML private VBox vboxStatistics;

    private Timeline masterTimeline;

    @FXML
    public void initialize() {
        loadAuctionsAndStats();
        loadNotifications();
    }

    private void loadAuctionsAndStats() {
        ApiService.getAsync("/auctions").thenAccept(res -> {
            Platform.runLater(() -> {
                try {
                    if (res.statusCode() == 200) {
                        ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        if (apiRes.code == 1000) {
                            Type listType = new TypeToken<List<AuctionModel>>() {}.getType();
                            List<AuctionModel> allAuctions = ApiService.gson.fromJson(apiRes.result, listType);

                            // 1. "Sản phẩm nổi bật": Sắp xếp theo GIÁ THẦU cao nhất
                            List<AuctionModel> featuredAuctions = allAuctions.stream()
                                    .filter(a -> "RUNNING".equals(a.status) || "OPEN".equals(a.status))
                                    .sorted((a1, a2) -> Double.compare(a2.highestBid, a1.highestBid))
                                    .limit(4)
                                    .collect(Collectors.toList());

                            // 2. "Thống kê": Sắp xếp theo TỶ LỆ PHẦN TRĂM (%) tăng trưởng
                            List<AuctionModel> growthStats = allAuctions.stream()
                                    .filter(a -> "RUNNING".equals(a.status) || "OPEN".equals(a.status))
                                    .sorted((a1, a2) -> {
                                        double s1 = (a1.bidProduct != null) ? a1.bidProduct.startPrice : 0;
                                        double p1 = (s1 == 0) ? 0 : (a1.highestBid - s1) / s1;
                                        double s2 = (a2.bidProduct != null) ? a2.bidProduct.startPrice : 0;
                                        double p2 = (s2 == 0) ? 0 : (a2.highestBid - s2) / s2;
                                        return Double.compare(p2, p1); // Giảm dần theo %
                                    })
                                    .limit(4)
                                    .collect(Collectors.toList());

                            vboxFeaturedAuctions.getChildren().clear();
                            vboxStatistics.getChildren().clear();

                            if (masterTimeline != null) masterTimeline.stop();
                            List<Runnable> timerTasks = new ArrayList<>();

                            // HIỂN THỊ KHU VỰC SẢN PHẨM NỔI BẬT
                            for (int i = 0; i < 4; i++) {
                                HBox row = new HBox(20);
                                row.setAlignment(Pos.CENTER_LEFT);
                                if (i < featuredAuctions.size()) {
                                    AuctionModel a = featuredAuctions.get(i);
                                    row.setStyle("-fx-background-color: #F8F9FB; -fx-background-radius: 15; -fx-padding: 10 25; -fx-min-height: 55; -fx-cursor: hand;");

                                    String shortId = a.bidProduct != null && a.bidProduct.id.length() >= 4
                                            ? a.bidProduct.id.substring(0, 4).toUpperCase() : "N/A";
                                    String name = a.bidProduct != null ? a.bidProduct.name : "Sản phẩm ẩn";

                                    Label lblId = new Label("SP" + shortId);
                                    lblId.setPrefWidth(80);
                                    lblId.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");

                                    Label lblName = new Label(name);
                                    lblName.setStyle("-fx-font-size: 16px; -fx-text-fill: #333;");

                                    Region spacer = new Region();
                                    HBox.setHgrow(spacer, Priority.ALWAYS);

                                    Label lblPrice = new Label(String.format("%,.0f VND", a.highestBid).replace(",", "."));
                                    lblPrice.setStyle("-fx-background-color: #0A439D; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 6 20; -fx-font-weight: bold; -fx-font-size: 15px;");

                                    String baseColor = "OPEN".equals(a.status) ? "#3498db" : "#f39c12";
                                    String targetTimeStr = "OPEN".equals(a.status) ? a.startTime : a.endTime;

                                    Label lblTime = new Label("00:00:00");
                                    lblTime.setStyle("-fx-background-color: " + baseColor + "; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 6 20; -fx-font-weight: bold; -fx-font-size: 15px;");

                                    if (targetTimeStr != null) {
                                        String timeStr = targetTimeStr.contains("T") ? targetTimeStr : targetTimeStr.replace(" ", "T");
                                        LocalDateTime targetTime = LocalDateTime.parse(timeStr);
                                        timerTasks.add(() -> {
                                            LocalDateTime now = LocalDateTime.now();
                                            if (now.isAfter(targetTime)) {
                                                lblTime.setText("00:00:00");
                                                lblTime.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 6 20; -fx-font-weight: bold; -fx-font-size: 15px;");
                                            } else {
                                                java.time.Duration duration = java.time.Duration.between(now, targetTime);
                                                lblTime.setText(String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart()));
                                                if ("RUNNING".equals(a.status) && duration.toHours() == 0 && duration.toMinutesPart() < 10) {
                                                    lblTime.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 6 20; -fx-font-weight: bold; -fx-font-size: 15px;");
                                                }
                                            }
                                        });
                                    }

                                    row.setOnMouseClicked(event -> {
                                        try {
                                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/AuctionDetail.fxml"));
                                            Node view = loader.load();
                                            ((AuctionDetailController)loader.getController()).setAuctionData(a);
                                            Pane contentArea = (Pane) row.getScene().lookup("#contentArea");
                                            if (contentArea != null) contentArea.getChildren().setAll(view);
                                        } catch (IOException e) { e.printStackTrace(); }
                                    });
                                    row.getChildren().addAll(lblId, lblName, spacer, lblPrice, lblTime);
                                } else {
                                    row.setStyle("-fx-background-color: transparent; -fx-min-height: 55;");
                                }
                                vboxFeaturedAuctions.getChildren().add(row);
                            }

                            // HIỂN THỊ KHU VỰC THỐNG KÊ TĂNG TRƯỞNG (ĐÃ SỬA MÀU VÀ MŨI TÊN)
                            if (growthStats.isEmpty()) {
                                vboxStatistics.getChildren().add(new Label("Chưa có dữ liệu thống kê."));
                            } else {
                                for (AuctionModel a : growthStats) {
                                    String name = a.bidProduct != null ? a.bidProduct.name : "Sản phẩm ẩn";
                                    double startPrice = a.bidProduct != null ? a.bidProduct.startPrice : 0;
                                    double growthAmount = a.highestBid - startPrice;
                                    double percent = (startPrice == 0) ? 0 : (growthAmount / startPrice) * 100;

                                    String statusColor = (percent > 100) ? "#e74c3c" : "#2ecc71";
                                    String arrow = (percent > 100) ? " ↑" : "";

                                    VBox statBox = new VBox(8);
                                    statBox.setStyle("-fx-background-color: #F8F9FB; -fx-background-radius: 10; -fx-padding: 12 15;");

                                    HBox header = new HBox();
                                    Label lblStatName = new Label(name);
                                    lblStatName.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #333;");
                                    Region statSpacer = new Region();
                                    HBox.setHgrow(statSpacer, Priority.ALWAYS);
                                    Label lblGrowth = new Label(String.format("+%,.0f VND", growthAmount).replace(",", "."));
                                    lblGrowth.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: " + statusColor + ";");
                                    header.getChildren().addAll(lblStatName, statSpacer, lblGrowth);

                                    HBox barRow = new HBox(10);
                                    barRow.setAlignment(Pos.CENTER_LEFT);
                                    Label lblStart = new Label(String.format("Từ: %,.0f", startPrice).replace(",", "."));
                                    lblStart.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");
                                    lblStart.setPrefWidth(100);

                                    ProgressBar pb = new ProgressBar(Math.min(percent / 100.0, 1.0));
                                    pb.setMaxWidth(Double.MAX_VALUE);
                                    HBox.setHgrow(pb, Priority.ALWAYS);
                                    pb.getStyleClass().setAll("progress-bar", "modern-progress-bar");
                                    pb.setStyle("-fx-accent: " + statusColor + "; -fx-background-insets: 0;");

                                    Label lblPercentValue = new Label(String.format("+%.1f%%%s", percent, arrow));
                                    lblPercentValue.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: " + statusColor + ";");
                                    lblPercentValue.setPrefWidth(85);
                                    lblPercentValue.setAlignment(Pos.CENTER_RIGHT);

                                    barRow.getChildren().addAll(lblStart, pb, lblPercentValue);
                                    statBox.getChildren().addAll(header, barRow);
                                    vboxStatistics.getChildren().add(statBox);
                                }
                            }

                            if (!timerTasks.isEmpty()) {
                                masterTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                                    for (Runnable task : timerTasks) task.run();
                                }));
                                masterTimeline.setCycleCount(Timeline.INDEFINITE);
                                masterTimeline.play();
                            }
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
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
                            Type listType = new TypeToken<List<NotificationModel>>() {}.getType();
                            List<NotificationModel> allNotifs = ApiService.gson.fromJson(apiRes.result, listType);

                            long unread = allNotifs.stream().filter(n -> !n.isRead).count();
                            lblNotifCount.setText(unread > 9 ? "9+" : String.valueOf(unread));
                            lblNotifCount.setVisible(unread > 0);

                            vboxNotifications.getChildren().clear();
                            int limit = Math.min(allNotifs.size(), 2);
                            for (int i = 0; i < limit; i++) {
                                NotificationModel n = allNotifs.get(i);
                                HBox row = new HBox(15);
                                row.setAlignment(Pos.CENTER_LEFT);
                                row.setStyle("-fx-background-color: #F8F9FB; -fx-background-radius: 12; -fx-padding: 20; -fx-min-height: 100;");

                                VBox textVBox = new VBox(8);
                                // SỬA: Cho chữ chiếm hết chiều ngang khung
                                HBox.setHgrow(textVBox, Priority.ALWAYS);

                                Label lblTitle = new Label(n.title);
                                lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 17px; -fx-text-fill: #333;");

                                Label lblDesc = new Label(n.description);
                                lblDesc.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
                                lblDesc.setWrapText(true);
                                // SỬA: Bỏ giới hạn chiều rộng 220, để chữ dài thoải mái
                                lblDesc.setMaxWidth(Double.MAX_VALUE);

                                textVBox.getChildren().addAll(lblTitle, lblDesc);

                                Button btnDelete = new Button("🗑");
                                btnDelete.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-background-radius: 50; -fx-min-width: 35; -fx-min-height: 35; -fx-cursor: hand;");
                                btnDelete.setOnAction(e -> processNotificationAction(n.notificationId, "delete"));

                                row.getChildren().addAll(textVBox, btnDelete);
                                vboxNotifications.getChildren().add(row);
                            }
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
            });
        });
    }

    private void processNotificationAction(String notifId, String actionType) {
        ApiService.deleteAsync("/notifications/" + notifId).thenAccept(res -> {
            if (res.statusCode() == 200) Platform.runLater(this::loadNotifications);
        });
    }

    private void switchView(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            Pane contentArea = (Pane) ((Node) event.getSource()).getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML public void goToAuctionList(ActionEvent event) { switchView(event, "/com/auction/view/AuctionList.fxml"); }
    @FXML public void handleViewMoreNotifications(ActionEvent event) { switchView(event, "/com/auction/view/NotificationList.fxml"); }
}
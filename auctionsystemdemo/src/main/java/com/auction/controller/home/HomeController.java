package com.auction.controller.home;


import lombok.extern.slf4j.Slf4j;
import com.auction.controller.auction.AuctionDetailController;
import com.auction.model.ApiResponse;
import com.auction.model.AuctionModel;
import com.auction.model.NotificationModel;
import com.auction.util.ApiService;
import com.auction.util.GlobalWebSocketManager;
import com.auction.util.SessionManager;
import com.google.gson.reflect.TypeToken;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextInputDialog;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class HomeController {

    @FXML private VBox vboxFeaturedAuctions;
    @FXML private VBox vboxNotifications;
    @FXML private Label lblNotifCount;
    @FXML private VBox vboxStatistics;

    private Timeline masterTimeline;
    private PauseTransition wsDebouncer = new PauseTransition(Duration.millis(400));

    @FXML
    public void initialize() {
        log.info("\u25B6 Controller Action - Execute: initialize()");
        // 1. Tải dữ liệu API lần đầu như bình thường
        loadAuctionsAndStats();
        loadNotifications();

        // =================================================================
        // 2. DELAY 1 GIÂY ĐỂ ĐẢM BẢO WEBSOCKET ĐÃ KẾT NỐI XONG HOÀN TOÀN
        // =================================================================
        PauseTransition delayWS = new PauseTransition(Duration.seconds(1));
        delayWS.setOnFinished(event -> {
            GlobalWebSocketManager.listenToGlobalAuctions(() -> {
                Platform.runLater(() -> {
                    wsDebouncer.setOnFinished(e -> {
                        log.info("⚡ WS HOME: Có giá mới, đang cập nhật lại Trang chủ ngầm...");
                        loadAuctionsAndStats();
                    });
                    wsDebouncer.playFromStart();
                });
            });
        });
        delayWS.play(); // Bắt đầu đếm ngược 1 giây

        // 3. Tự động gỡ kết nối khi người dùng rời khỏi trang chủ
        vboxFeaturedAuctions.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                GlobalWebSocketManager.stopListeningGlobalAuctions();
                if (masterTimeline != null) masterTimeline.stop();
            }
        });
    }

    private void loadAuctionsAndStats() {
        log.info("\u25B6 Controller Action - Execute: loadAuctionsAndStats()");
        ApiService.getAsync("/auctions").thenAccept(res -> {
            Platform.runLater(() -> {
                try {
                    if (res.statusCode() == 200) {
                        ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        if (apiRes.code == 1000) {
                            Type listType = new TypeToken<List<AuctionModel>>(){}.getType();
                            List<AuctionModel> allAuctions = ApiService.gson.fromJson(apiRes.result, listType);

                            List<AuctionModel> topAuctions = allAuctions.stream()
                                    .filter(a -> "RUNNING".equals(a.status) || "OPEN".equals(a.status))
                                    .sorted((a1, a2) -> Double.compare(a2.highestBid, a1.highestBid))
                                    .limit(4)
                                    .collect(Collectors.toList());

                            vboxFeaturedAuctions.getChildren().clear();
                            vboxStatistics.getChildren().clear();

                            if (masterTimeline != null) masterTimeline.stop();
                            List<Runnable> timerTasks = new ArrayList<>();

                            for (int i = 0; i < 4; i++) {
                                HBox row = new HBox(20);
                                row.setAlignment(Pos.CENTER_LEFT);

                                if (i < topAuctions.size()) {
                                    AuctionModel a = topAuctions.get(i);

                                    row.getStyleClass().add("custom-row");
                                    row.setStyle("-fx-padding: 10 25; -fx-min-height: 55; -fx-cursor: hand;");

                                    String shortId = a.bidProduct != null && a.bidProduct.id.length() >= 4
                                            ? a.bidProduct.id.substring(0, 4).toUpperCase() : "N/A";
                                    String name = a.bidProduct != null ? a.bidProduct.name : "Sản phẩm ẩn";

                                    Label lblId = new Label("SP" + shortId);
                                    lblId.setPrefWidth(80);
                                    lblId.getStyleClass().add("row-title-bold");

                                    Label lblName = new Label(name);
                                    lblName.getStyleClass().add("row-text-normal");

                                    Region spacer = new Region();
                                    HBox.setHgrow(spacer, Priority.ALWAYS);

                                    Label lblPrice = new Label(String.format("%,.0f VND", a.highestBid).replace(",", "."));
                                    lblPrice.setStyle("-fx-background-color: #0A439D; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 6 20; -fx-font-weight: bold; -fx-font-size: 15px;");

                                    String baseColor = "OPEN".equals(a.status) ? "#3498db" : "#f39c12";
                                    String prefixText = "OPEN".equals(a.status) ? "Sắp bắt đầu sau:" : "Thời gian còn lại:";
                                    String targetTimeStr = "OPEN".equals(a.status) ? a.startTime : a.endTime;

                                    Label lblTimePrefix = new Label(prefixText);
                                    lblTimePrefix.getStyleClass().add("row-text-muted");

                                    Label lblTime = new Label("00:00:00");
                                    lblTime.setStyle("-fx-background-color: " + baseColor + "; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 6 20; -fx-font-weight: bold; -fx-font-size: 15px;");

                                    if (targetTimeStr != null) {
                                        String timeStr = targetTimeStr.contains("T") ? targetTimeStr : targetTimeStr.replace(" ", "T");
                                        // DÙNG HÀM XỬ LÝ THỜI GIAN AN TOÀN TRÁNH CRASH (Giống Detail)
                                        LocalDateTime targetTime;
                                        try {
                                            if (timeStr.contains(".")) timeStr = timeStr.substring(0, timeStr.indexOf("."));
                                            if (timeStr.endsWith("Z")) timeStr = timeStr.replace("Z", "");
                                            if (timeStr.contains("+")) timeStr = timeStr.substring(0, timeStr.indexOf("+"));
                                            targetTime = LocalDateTime.parse(timeStr);
                                        } catch (Exception e) { targetTime = LocalDateTime.now(); }

                                        LocalDateTime finalTargetTime = targetTime;
                                        timerTasks.add(() -> {
                                            LocalDateTime now = LocalDateTime.now();
                                            if (now.isAfter(finalTargetTime)) {
                                                lblTime.setText("00:00:00");
                                                lblTime.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 6 20; -fx-font-weight: bold; -fx-font-size: 15px;");
                                            } else {
                                                java.time.Duration duration = java.time.Duration.between(now, finalTargetTime);
                                                long hours = duration.toHours();
                                                long minutes = duration.toMinutesPart();
                                                long seconds = duration.toSecondsPart();
                                                lblTime.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));

                                                if ("RUNNING".equals(a.status) && hours == 0 && minutes < 10) {
                                                    lblTime.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 6 20; -fx-font-weight: bold; -fx-font-size: 15px;");
                                                }
                                            }
                                        });
                                    }

                                    row.setOnMouseClicked(event -> {
                                        try {
                                            GlobalWebSocketManager.stopListeningGlobalAuctions(); // Ngắt cáp mạng khi qua chi tiết
                                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/auction/AuctionDetail.fxml"));
                                            Node view = loader.load();

                                            AuctionDetailController detailController = loader.getController();
                                            detailController.setAuctionData(a);

                                            Pane contentArea = (Pane) row.getScene().lookup("#contentArea");
                                            if (contentArea != null) {
                                                contentArea.getChildren().setAll(view);
                                            }
                                        } catch (IOException e) {
                                            log.error("Exception occurred", e);
                                        }
                                    });

                                    row.getChildren().addAll(lblId, lblName, spacer, lblPrice, lblTimePrefix, lblTime);

                                } else {
                                    row.setStyle("-fx-background-color: transparent; -fx-min-height: 55;");
                                }
                                vboxFeaturedAuctions.getChildren().add(row);
                            }

                            if (topAuctions.isEmpty()) {
                                Label emptyLbl = new Label("Chưa có dữ liệu thống kê.");
                                emptyLbl.getStyleClass().add("row-text-muted");
                                vboxStatistics.getChildren().add(emptyLbl);
                            } else {
                                for (AuctionModel a : topAuctions) {
                                    String name = a.bidProduct != null ? a.bidProduct.name : "Sản phẩm ẩn";
                                    double startPrice = a.bidProduct != null ? a.bidProduct.startPrice : 0;
                                    double currentPrice = a.highestBid;

                                    double growthAmount = currentPrice - startPrice;
                                    double percent = startPrice == 0 ? 0 : (growthAmount / startPrice) * 100;
                                    double progress = startPrice == 0 ? 0 : Math.min(growthAmount / startPrice, 1.0);

                                    String colorHex = (percent > 100) ? "#FF4D4D" : "#2ECC71";
                                    String percentText = (percent > 100) ? String.format("+%.1f%% ↑", percent) : String.format("+%.1f%%", percent);

                                    VBox statBox = new VBox(8);
                                    statBox.getStyleClass().add("custom-row");
                                    statBox.setStyle("-fx-padding: 12 15;");

                                    HBox header = new HBox();
                                    Label lblStatName = new Label(name);
                                    lblStatName.getStyleClass().add("row-title-bold");

                                    Region statSpacer = new Region();
                                    HBox.setHgrow(statSpacer, Priority.ALWAYS);

                                    Label lblGrowth = new Label(String.format("+%,.0f VND", growthAmount).replace(",", "."));
                                    lblGrowth.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: " + colorHex + ";");
                                    header.getChildren().addAll(lblStatName, statSpacer, lblGrowth);

                                    HBox barRow = new HBox(10);
                                    barRow.setAlignment(Pos.CENTER_LEFT);

                                    Label lblStart = new Label(String.format("Từ: %,.0f", startPrice).replace(",", "."));
                                    lblStart.getStyleClass().add("row-text-muted");
                                    lblStart.setPrefWidth(110);

                                    ProgressBar pb = new ProgressBar(progress);
                                    pb.setMaxWidth(Double.MAX_VALUE);
                                    HBox.setHgrow(pb, Priority.ALWAYS);

                                    pb.getStyleClass().removeAll("modern-progress-bar", "progress-alert", "progress-normal", "red-progress-bar");
                                    pb.getStyleClass().add(percent > 100 ? "progress-alert" : "progress-normal");

                                    Label lblPercent = new Label(percentText);
                                    lblPercent.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: " + colorHex + ";");
                                    lblPercent.setPrefWidth(70);
                                    lblPercent.setAlignment(Pos.CENTER_RIGHT);

                                    barRow.getChildren().addAll(lblStart, pb, lblPercent);
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
                } catch (Exception e) {
                    log.info("Lỗi parse JSON ở Dashboard: " + e.getMessage());
                }
            });
        });
    }

    private void loadNotifications() {
        log.info("\u25B6 Controller Action - Execute: loadNotifications()");
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
                                lbl.getStyleClass().add("row-text-muted");
                                vboxNotifications.getChildren().add(lbl);
                                return;
                            }

                            int limit = Math.min(allNotifs.size(), 2);
                            for (int i = 0; i < limit; i++) {
                                NotificationModel n = allNotifs.get(i);
                                HBox row = new HBox(15);
                                row.setAlignment(Pos.CENTER_LEFT);

                                row.getStyleClass().add("custom-row");
                                row.setStyle("-fx-padding: 15;");

                                VBox textVBox = new VBox(5);
                                Label lblTitle = new Label(n.title);
                                lblTitle.getStyleClass().add("row-title-bold");

                                Label lblDesc = new Label(n.description);
                                lblDesc.getStyleClass().add("row-text-normal");
                                lblDesc.setWrapText(true);
                                lblDesc.setMaxWidth(Double.MAX_VALUE);

                                textVBox.getChildren().addAll(lblTitle, lblDesc);
                                HBox.setHgrow(textVBox, Priority.ALWAYS);

                                Region spacer = new Region();
                                HBox.setHgrow(spacer, Priority.ALWAYS);

                                HBox actionBox = new HBox(5);
                                actionBox.setAlignment(Pos.CENTER_RIGHT);

                                if ("PAYMENT_VERIFICATION".equals(n.type) || "FRIEND_REQUEST".equals(n.type) || "UPGRADE_REQUEST".equals(n.type)) {
                                    Button btnAccept = new Button("✔");
                                    btnAccept.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-background-radius: 50; -fx-min-width: 30; -fx-min-height: 30; -fx-cursor: hand; -fx-font-weight: bold;");
                                    btnAccept.setOnAction(e -> processNotificationAction(n.notificationId, "accept", n.type));

                                    Button btnDecline = new Button("✖");
                                    btnDecline.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 50; -fx-min-width: 30; -fx-min-height: 30; -fx-cursor: hand; -fx-font-weight: bold;");
                                    btnDecline.setOnAction(e -> processNotificationAction(n.notificationId, "decline", n.type));

                                    actionBox.getChildren().addAll(btnAccept, btnDecline);
                                } else {
                                    Button btnDelete = new Button("🗑");
                                    btnDelete.getStyleClass().add("btn-delete-icon");
                                    btnDelete.setStyle("-fx-background-radius: 50; -fx-min-width: 30; -fx-min-height: 30; -fx-cursor: hand; -fx-font-weight: bold;");
                                    btnDelete.setOnAction(e -> processNotificationAction(n.notificationId, "delete", n.type));

                                    actionBox.getChildren().add(btnDelete);
                                }

                                row.getChildren().addAll(textVBox, spacer, actionBox);
                                vboxNotifications.getChildren().add(row);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.info("Lỗi load Thông báo ở Dashboard.");
                }
            });
        });
    }

    private void processNotificationAction(String notifId, String actionType, String notifType) {
        String endpoint = "/notifications/" + notifId;

        if ("accept".equals(actionType)) {
            endpoint += "/accept";
            String tempMsg = "Xác nhận thành công!";
            if ("UPGRADE_REQUEST".equals(notifType)) tempMsg = "Đã phê duyệt yêu cầu lên Seller!";
            else if ("FRIEND_REQUEST".equals(notifType)) tempMsg = "Đã chấp nhận kết bạn!";
            else if ("PAYMENT_VERIFICATION".equals(notifType)) tempMsg = "Xác nhận thanh toán thành công!";

            final String finalMsg = tempMsg;
            ApiService.putAsync(endpoint, null).thenAccept(res -> handleActionResponse(res.statusCode(), finalMsg));

        } else if ("decline".equals(actionType)) {
            if ("UPGRADE_REQUEST".equals(notifType)) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Từ chối yêu cầu");
                dialog.setHeaderText("Từ chối cấp quyền Seller");
                dialog.setContentText("Nhập lý do từ chối:");
                com.auction.util.AlertUtils.applyStyle(dialog);

                dialog.showAndWait().ifPresent(reason -> {
                    if (reason.trim().isEmpty()) {
                        Alert alert = new Alert(Alert.AlertType.WARNING, "Bạn bắt buộc phải nhập lý do!");
                        com.auction.util.AlertUtils.applyStyle(alert);
                        alert.showAndWait();
                        return;
                    }
                    Map<String, String> body = new HashMap<>();
                    body.put("reason", reason.trim());

                    ApiService.putAsync("/notifications/" + notifId + "/decline", body)
                            .thenAccept(res -> handleActionResponse(res.statusCode(), "Đã gửi thông báo từ chối tới người dùng!"));
                });
            } else {
                endpoint += "/decline";
                String tempMsg = "FRIEND_REQUEST".equals(notifType) ? "Đã từ chối kết bạn!" : "Đã từ chối thanh toán!";
                final String finalMsg = tempMsg;
                ApiService.putAsync(endpoint, null).thenAccept(res -> handleActionResponse(res.statusCode(), finalMsg));
            }
        } else if ("delete".equals(actionType)) {
            ApiService.deleteAsync(endpoint).thenAccept(res -> handleActionResponse(res.statusCode(), null));
        }
    }

    private void handleActionResponse(int statusCode, String successMsg) {
        log.info("\u25B6 Controller Action - Execute: handleActionResponse()");
        Platform.runLater(() -> {
            if (statusCode >= 200 && statusCode < 300) {
                if (successMsg != null) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setHeaderText(null);
                    alert.setContentText(successMsg);
                    com.auction.util.AlertUtils.applyStyle(alert);
                    alert.showAndWait();
                }
                loadNotifications();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText(null);
                alert.setContentText("Thao tác thất bại! Mã lỗi: " + statusCode);
                com.auction.util.AlertUtils.applyStyle(alert);
                alert.showAndWait();
            }
        });
    }

    private void switchView(ActionEvent event, String fxmlPath) {
        GlobalWebSocketManager.stopListeningGlobalAuctions(); // Ngắt WebSocket khi chuyển trang
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            Node source = (Node) event.getSource();
            Pane contentArea = (Pane) source.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (IOException e) {
            log.error("Exception occurred", e);
        }
    }

    @FXML
    public void goToAuctionList(ActionEvent event) {
        log.info("\u25B6 Controller Action - Execute: goToAuctionList()");
        switchView(event, "/com/auction/view/auction/AuctionList.fxml");
    }

    @FXML
    public void handleViewMoreNotifications(ActionEvent event) {
        log.info("\u25B6 Controller Action - Execute: handleViewMoreNotifications()");
        switchView(event, "/com/auction/view/notification/NotificationList.fxml");
    }
}
package com.auction.controller.search;

import com.auction.model.ApiResponse;
import com.auction.model.AuctionModel;
import com.auction.model.ConnectionModel;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SearchController {

    @FXML private Label lblKeyword;
    @FXML private FlowPane paneItems;
    @FXML private VBox paneUsers;

    // ===============================================
    // XỬ LÝ SỰ KIỆN QUAY LẠI TRANG CHỦ
    // ===============================================
    @FXML
    public void handleGoBack(MouseEvent event) {
        if (com.auction.controller.dashboard.MainController.getInstance() != null) {
            com.auction.controller.dashboard.MainController.getInstance().showDashboard(null);
        }
    }

    public void executeSearch(String keyword) {
        lblKeyword.setText("'" + keyword + "'");

        // 1. Tìm Sản phẩm đấu giá
        searchAuctions(keyword);

        // 2. Tìm Người dùng
        searchUsers(keyword);
    }

    // ===============================================
    // RENDER THẺ SẢN PHẨM ĐẤU GIÁ (UI HIỆN ĐẠI)
    // ===============================================
    private void searchAuctions(String keyword) {
        paneItems.getChildren().clear();
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);

        ApiService.getAsync("/auctions/search?keyword=" + encodedKeyword).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        java.lang.reflect.Type listType = new TypeToken<List<AuctionModel>>(){}.getType();
                        List<AuctionModel> list = ApiService.gson.fromJson(apiRes.result, listType);

                        if (list == null || list.isEmpty()) {
                            paneItems.getChildren().add(new Label("Không tìm thấy sản phẩm nào."));
                        } else {
                            for (AuctionModel auc : list) {
                                // VẼ CARD SẢN PHẨM
                                VBox card = new VBox(8);
                                card.setPrefWidth(260); // Rộng cố định
                                card.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 4); -fx-cursor: hand; -fx-border-color: transparent; -fx-border-radius: 15;");

                                // Hiệu ứng Hover
                                card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-border-color: #0A439D; -fx-border-width: 1.5px; -fx-background-color: #f8fbff;"));
                                card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-border-color: #0A439D; -fx-border-width: 1.5px; -fx-background-color: #f8fbff;", "")));

                                // Icon bọc trong vòng tròn nền nhẹ
                                StackPane iconContainer = new StackPane();
                                iconContainer.setStyle("-fx-background-color: #e8f0fe; -fx-background-radius: 50; -fx-min-width: 50; -fx-max-width: 50; -fx-min-height: 50; -fx-max-height: 50;");
                                Label icon = new Label("📦");
                                icon.setStyle("-fx-font-size: 24px;");
                                iconContainer.getChildren().add(icon);

                                Label name = new Label(auc.bidProduct.name);
                                name.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");
                                name.setWrapText(true);

                                VBox priceBox = new VBox(2);
                                Label priceLabel = new Label("Giá cao nhất hiện tại:");
                                priceLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 12px;");
                                Label priceValue = new Label(String.format("%,.0f đ", auc.highestBid));
                                priceValue.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 18px;");
                                priceBox.getChildren().addAll(priceLabel, priceValue);

                                Region spacer = new Region();
                                VBox.setVgrow(spacer, Priority.ALWAYS); // Đẩy giá xuống đáy card

                                card.getChildren().addAll(iconContainer, name, spacer, priceBox);
                                paneItems.getChildren().add(card);
                            }
                        }
                    }
                } else {
                    paneItems.getChildren().add(new Label("Lỗi máy chủ: " + res.statusCode()));
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> paneItems.getChildren().add(new Label("Mất kết nối tới máy chủ!")));
            return null;
        });
    }

    // ===============================================
    // RENDER DANH SÁCH NGƯỜI DÙNG VÀ XỬ LÝ KẾT BẠN
    // ===============================================
    private void searchUsers(String keyword) {
        paneUsers.getChildren().clear();
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);

        ApiService.getAsync("/users/search?keyword=" + encodedKeyword).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        java.lang.reflect.Type listType = new TypeToken<List<ConnectionModel.UserModel>>(){}.getType();
                        List<ConnectionModel.UserModel> list = ApiService.gson.fromJson(apiRes.result, listType);

                        if (list == null || list.isEmpty()) {
                            paneUsers.getChildren().add(new Label("Không tìm thấy người dùng nào."));
                        } else {
                            for (ConnectionModel.UserModel user : list) {
                                paneUsers.getChildren().add(createUserRow(user));
                            }
                        }
                    }
                } else {
                    paneUsers.getChildren().add(new Label("Lỗi máy chủ: " + res.statusCode()));
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> paneUsers.getChildren().add(new Label("Mất kết nối tới máy chủ!")));
            return null;
        });
    }

    private HBox createUserRow(ConnectionModel.UserModel user) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);

        // THAY ĐỔI: Thêm class chung để quản lý theme, tách màu nền (đưa vào CSS) ra khỏi cấu trúc layout
        row.getStyleClass().add("user-search-card");
        row.setStyle("-fx-padding: 15 25; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 8, 0, 0, 3);");

        // Avatar
        ImageView avt = new ImageView(new Image(ApiService.BASE_URL + user.avatarUrl, true));
        avt.setFitWidth(50); avt.setFitHeight(50);
        Rectangle clip = new Rectangle(50, 50); clip.setArcWidth(50); clip.setArcHeight(50);
        avt.setClip(clip);

        // Thông tin User
        VBox info = new VBox(2);
        info.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label(user.fullName != null ? user.fullName : user.userName);
        name.getStyleClass().add("user-card-title"); // THAY ĐỔI: Thêm class cho tên
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label username = new Label("@" + user.userName);
        username.getStyleClass().add("user-card-subtitle"); // THAY ĐỔI: Thêm class cho @username
        username.setStyle("-fx-font-size: 13px;");
        info.getChildren().addAll(name, username);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Nút trạng thái Mặc định
        Button btnSingleAction = new Button("Đang kiểm tra...");
        btnSingleAction.setDisable(true);
        // THAY ĐỔI: Đưa màu nút "Đang kiểm tra" thành class .btn-status-checking
        btnSingleAction.getStyleClass().add("btn-status-checking");
        btnSingleAction.setStyle("-fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20;");

        // Cụm 2 nút dành riêng cho trạng thái PENDING_RECEIVER
        HBox dualActionBox = new HBox(10);
        Button btnAccept = new Button("✓ Chấp nhận");
        btnAccept.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");

        Button btnDecline = new Button("✕ Từ chối");
        // THAY ĐỔI: Đưa nút từ chối vào class .btn-status-decline để đổi nền tối khi cần
        btnDecline.getStyleClass().add("btn-status-decline");
        btnDecline.setStyle("-fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");

        dualActionBox.getChildren().addAll(btnAccept, btnDecline);
        dualActionBox.setVisible(false);
        dualActionBox.setManaged(false);

        StackPane actionContainer = new StackPane(btnSingleAction, dualActionBox);

        if (user.userName.equals(SessionManager.userName)) {
            actionContainer.setVisible(false);
            actionContainer.setManaged(false);
        } else {
            System.out.println("Đang kiểm tra kết bạn giữa: [" + SessionManager.userName + "] và [" + user.userName + "]");

            ApiService.getAsync("/chat/check-connection?user1=" + SessionManager.userName + "&user2=" + user.userName).thenAccept(statusRes -> {
                Platform.runLater(() -> {
                    if (statusRes.statusCode() == 200) {
                        ApiResponse apiRes = ApiService.gson.fromJson(statusRes.body(), ApiResponse.class);

                        if (apiRes.code == 1000 && apiRes.result != null) {
                            String status = String.valueOf(apiRes.result).replaceAll("[^A-Z_]", "");

                            // Xóa các class trạng thái cũ trước khi gán mới tránh xung đột màu
                            btnSingleAction.getStyleClass().removeAll("btn-status-friends", "btn-status-pending", "btn-status-none");

                            switch (status) {
                                case "ACCEPTED":
                                    btnSingleAction.setVisible(true); btnSingleAction.setManaged(true);
                                    dualActionBox.setVisible(false); dualActionBox.setManaged(false);
                                    btnSingleAction.setText("Đã là bạn bè ✓");
                                    btnSingleAction.getStyleClass().add("btn-status-friends"); // THAY ĐỔI: Dùng class CSS
                                    break;
                                case "PENDING_SENDER":
                                    btnSingleAction.setVisible(true); btnSingleAction.setManaged(true);
                                    dualActionBox.setVisible(false); dualActionBox.setManaged(false);
                                    btnSingleAction.setText("Đã gửi lời mời");
                                    btnSingleAction.getStyleClass().add("btn-status-pending"); // THAY ĐỔI: Dùng class CSS
                                    break;
                                case "PENDING_RECEIVER":
                                    btnSingleAction.setVisible(false);
                                    btnSingleAction.setManaged(false);
                                    dualActionBox.setVisible(true);
                                    dualActionBox.setManaged(true);
                                    break;
                                case "NONE":
                                default:
                                    btnSingleAction.setVisible(true); btnSingleAction.setManaged(true);
                                    dualActionBox.setVisible(false); dualActionBox.setManaged(false);
                                    btnSingleAction.setText("➕ Kết bạn");
                                    btnSingleAction.getStyleClass().add("btn-status-none"); // THAY ĐỔI: Dùng class CSS
                                    btnSingleAction.setDisable(false);
                                    break;
                            }
                        }
                    }
                });
            });

            btnSingleAction.setOnAction(e -> {
                btnSingleAction.setText("Đang gửi...");
                btnSingleAction.setDisable(true);
                ApiService.postAsync("/chat/friend-request?sender=" + SessionManager.userName + "&receiver=" + user.userName, null).thenAccept(res -> {
                    Platform.runLater(() -> {
                        if (res.statusCode() >= 200 && res.statusCode() < 300) {
                            btnSingleAction.setText("Đã gửi lời mời");
                            btnSingleAction.getStyleClass().removeAll("btn-status-none");
                            btnSingleAction.getStyleClass().add("btn-status-pending");
                        } else {
                            btnSingleAction.setText("➕ Kết bạn");
                            btnSingleAction.setDisable(false);
                            showAlert("Lỗi", "Không thể gửi lời mời kết bạn.");
                        }
                    });
                });
            });

            btnAccept.setOnAction(e -> {
                btnAccept.setText("Đang xử lý...");
                btnAccept.setDisable(true);
                btnDecline.setDisable(true);

                ApiService.putAsync("/chat/accept-request?sender=" + user.userName + "&receiver=" + SessionManager.userName, null).thenAccept(res -> {
                    Platform.runLater(() -> {
                        if (res.statusCode() >= 200 && res.statusCode() < 300) {
                            dualActionBox.setVisible(false); dualActionBox.setManaged(false);
                            btnSingleAction.setVisible(true); btnSingleAction.setManaged(true);
                            btnSingleAction.setText("Đã là bạn bè ✓");
                            btnSingleAction.getStyleClass().add("btn-status-friends");
                        } else {
                            btnAccept.setText("✓ Chấp nhận");
                            btnAccept.setDisable(false); btnDecline.setDisable(false);
                            showAlert("Lỗi", "Không thể chấp nhận yêu cầu.");
                        }
                    });
                });
            });

            btnDecline.setOnAction(e -> {
                btnDecline.setText("Đang xử lý...");
                btnDecline.setDisable(true);
                btnAccept.setDisable(true);

                ApiService.putAsync("/chat/decline-request?sender=" + user.userName + "&receiver=" + SessionManager.userName, null).thenAccept(res -> {
                    Platform.runLater(() -> {
                        if (res.statusCode() >= 200 && res.statusCode() < 300) {
                            dualActionBox.setVisible(false); dualActionBox.setManaged(false);
                            btnSingleAction.setVisible(true); btnSingleAction.setManaged(true);
                            btnSingleAction.setDisable(false);
                            btnSingleAction.setText("➕ Kết bạn");
                            btnSingleAction.getStyleClass().removeAll("btn-status-pending");
                            btnSingleAction.getStyleClass().add("btn-status-none");
                        } else {
                            btnDecline.setText("✕ Từ chối");
                            btnAccept.setDisable(false); btnDecline.setDisable(false);
                            showAlert("Lỗi", "Không thể từ chối yêu cầu.");
                        }
                    });
                });
            });
        }

        row.getChildren().addAll(avt, info, spacer, actionContainer);
        return row;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}
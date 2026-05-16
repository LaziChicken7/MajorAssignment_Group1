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
        row.setStyle("-fx-background-color: white; -fx-padding: 15 25; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 8, 0, 0, 3);");

        // Avatar
        ImageView avt = new ImageView(new Image(ApiService.BASE_URL + user.avatarUrl, true));
        avt.setFitWidth(50); avt.setFitHeight(50);
        Rectangle clip = new Rectangle(50, 50); clip.setArcWidth(50); clip.setArcHeight(50);
        avt.setClip(clip);

        // Thông tin User
        VBox info = new VBox(2);
        info.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(user.fullName != null ? user.fullName : user.userName);
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");
        Label username = new Label("@" + user.userName);
        username.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
        info.getChildren().addAll(name, username);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Nút trạng thái Mặc định
        Button btnSingleAction = new Button("Đang kiểm tra...");
        btnSingleAction.setDisable(true);
        btnSingleAction.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20;");

        // Cụm 2 nút dành riêng cho trạng thái PENDING_RECEIVER (Có người gửi lời mời đến)
        HBox dualActionBox = new HBox(10);
        Button btnAccept = new Button("✓ Chấp nhận");
        btnAccept.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");

        Button btnDecline = new Button("✕ Từ chối");
        btnDecline.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");

        dualActionBox.getChildren().addAll(btnAccept, btnDecline);
        dualActionBox.setVisible(false);
        dualActionBox.setManaged(false);

        // Vùng chứa nút (Để thay đổi linh hoạt giữa 1 nút và 2 nút)
        StackPane actionContainer = new StackPane(btnSingleAction, dualActionBox);

        if (user.userName.equals(SessionManager.userName)) {
            actionContainer.setVisible(false);
            actionContainer.setManaged(false);
        } else {
            // IN LOG RA ĐỂ KIỂM TRA XEM FRONTEND ĐANG GỬI LÊN CÁI GÌ
            System.out.println("Đang kiểm tra kết bạn giữa: [" + SessionManager.userName + "] và [" + user.userName + "]");

            // Kiểm tra trạng thái bạn bè
            ApiService.getAsync("/chat/check-connection?user1=" + SessionManager.userName + "&user2=" + user.userName).thenAccept(statusRes -> {
                Platform.runLater(() -> {
                    if (statusRes.statusCode() == 200) {
                        ApiResponse apiRes = ApiService.gson.fromJson(statusRes.body(), ApiResponse.class);

                        if (apiRes.code == 1000 && apiRes.result != null) {
                            // ÉP KIỂU AN TOÀN: Gọt sạch mọi ký tự lạ (dấu cách ẩn, ngoặc kép), chỉ giữ lại CHỮ HOA và DẤU GẠCH DƯỚI
                            String status = String.valueOf(apiRes.result).replaceAll("[^A-Z_]", "");

                            switch (status) {
                                case "ACCEPTED":
                                    btnSingleAction.setVisible(true); btnSingleAction.setManaged(true);
                                    dualActionBox.setVisible(false); dualActionBox.setManaged(false);
                                    btnSingleAction.setText("Đã là bạn bè ✓");
                                    btnSingleAction.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20;");
                                    break;
                                case "PENDING_SENDER":
                                    btnSingleAction.setVisible(true); btnSingleAction.setManaged(true);
                                    dualActionBox.setVisible(false); dualActionBox.setManaged(false);
                                    btnSingleAction.setText("Đã gửi lời mời");
                                    btnSingleAction.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20;");
                                    break;
                                case "PENDING_RECEIVER":
                                    // BẬT HIỂN THỊ CỤM 2 NÚT CHẤP NHẬN/TỪ CHỐI
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
                                    btnSingleAction.setStyle("-fx-background-color: #0A439D; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");
                                    btnSingleAction.setDisable(false);
                                    break;
                            }
                        }
                    }
                });
            });

            // ----------------------------------------------------
            // SỰ KIỆN 1: GỬI LỜI MỜI (Khi nút đang là "➕ Kết bạn")
            // ----------------------------------------------------
            btnSingleAction.setOnAction(e -> {
                btnSingleAction.setText("Đang gửi...");
                btnSingleAction.setDisable(true);
                ApiService.postAsync("/chat/friend-request?sender=" + SessionManager.userName + "&receiver=" + user.userName, null).thenAccept(res -> {
                    Platform.runLater(() -> {
                        if (res.statusCode() >= 200 && res.statusCode() < 300) {
                            btnSingleAction.setText("Đã gửi lời mời");
                            btnSingleAction.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20;");
                        } else {
                            btnSingleAction.setText("➕ Kết bạn");
                            btnSingleAction.setDisable(false);
                            showAlert("Lỗi", "Không thể gửi lời mời kết bạn.");
                        }
                    });
                });
            });

            // ----------------------------------------------------
            // SỰ KIỆN 2: CHẤP NHẬN (Gọi API /chat/accept-request)
            // LƯU Ý: sender ở đây là người kia (user.userName), receiver là chính mình
            // ----------------------------------------------------
            btnAccept.setOnAction(e -> {
                btnAccept.setText("Đang xử lý...");
                btnAccept.setDisable(true);
                btnDecline.setDisable(true);

                ApiService.putAsync("/chat/accept-request?sender=" + user.userName + "&receiver=" + SessionManager.userName, null).thenAccept(res -> {
                    Platform.runLater(() -> {
                        if (res.statusCode() >= 200 && res.statusCode() < 300) {
                            // Tắt khung 2 nút, bật lại khung 1 nút báo thành công
                            dualActionBox.setVisible(false); dualActionBox.setManaged(false);
                            btnSingleAction.setVisible(true); btnSingleAction.setManaged(true);
                            btnSingleAction.setText("Đã là bạn bè ✓");
                            btnSingleAction.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20;");
                        } else {
                            btnAccept.setText("✓ Chấp nhận");
                            btnAccept.setDisable(false); btnDecline.setDisable(false);
                            showAlert("Lỗi", "Không thể chấp nhận yêu cầu.");
                        }
                    });
                });
            });

            // ----------------------------------------------------
            // SỰ KIỆN 3: TỪ CHỐI (Gọi API /chat/decline-request)
            // LƯU Ý: sender ở đây là người kia (user.userName), receiver là chính mình
            // ----------------------------------------------------
            btnDecline.setOnAction(e -> {
                btnDecline.setText("Đang xử lý...");
                btnDecline.setDisable(true);
                btnAccept.setDisable(true);

                ApiService.putAsync("/chat/decline-request?sender=" + user.userName + "&receiver=" + SessionManager.userName, null).thenAccept(res -> {
                    Platform.runLater(() -> {
                        if (res.statusCode() >= 200 && res.statusCode() < 300) {
                            // Tắt khung 2 nút, bật lại nút "Kết bạn" như người lạ
                            dualActionBox.setVisible(false); dualActionBox.setManaged(false);
                            btnSingleAction.setVisible(true); btnSingleAction.setManaged(true);
                            btnSingleAction.setDisable(false);
                            btnSingleAction.setText("➕ Kết bạn");
                            btnSingleAction.setStyle("-fx-background-color: #0A439D; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");
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
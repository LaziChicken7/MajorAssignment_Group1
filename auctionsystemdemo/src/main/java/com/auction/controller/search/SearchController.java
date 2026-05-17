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

    @FXML
    public void handleGoBack(MouseEvent event) {
        if (com.auction.controller.dashboard.MainController.getInstance() != null) {
            com.auction.controller.dashboard.MainController.getInstance().showDashboard(null);
        }
    }

    public void executeSearch(String keyword) {
        lblKeyword.setText("'" + keyword + "'");
        searchAuctions(keyword);
        searchUsers(keyword);
    }

    // ===============================================
    // RENDER THẺ SẢN PHẨM ĐẤU GIÁ (UI HIỆN ĐẠI CÓ ẢNH)
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
                            Label empty = new Label("Không tìm thấy sản phẩm nào.");
                            empty.getStyleClass().add("muted-text");
                            paneItems.getChildren().add(empty);
                        } else {
                            for (AuctionModel auc : list) {
                                // 1. KHUNG CARD CHÍNH
                                VBox card = new VBox(10);
                                card.setPrefWidth(220); // Thu nhỏ lại chút để hiển thị được nhiều cột hơn
                                card.getStyleClass().addAll("card", "auction-search-card");
                                card.setStyle("-fx-padding: 15; -fx-cursor: hand;");

                                // 2. ẢNH SẢN PHẨM (NẾU CÓ)
                                ImageView imgView = new ImageView();
                                imgView.setFitWidth(190);
                                imgView.setFitHeight(150);
                                String imgPath = "https://via.placeholder.com/190x150?text=No+Image";
                                if (auc.bidProduct.imageUrls != null && !auc.bidProduct.imageUrls.isEmpty()) {
                                    imgPath = ApiService.BASE_URL + auc.bidProduct.imageUrls.get(0);
                                }
                                imgView.setImage(new Image(imgPath, true));

                                // Bo tròn góc ảnh
                                Rectangle clip = new Rectangle(190, 150);
                                clip.setArcWidth(15); clip.setArcHeight(15);
                                imgView.setClip(clip);

                                // 3. THÔNG TIN SẢN PHẨM
                                Label name = new Label(auc.bidProduct.name);
                                name.getStyleClass().add("row-title-bold"); // Chữ tự đổi đen/trắng
                                name.setStyle("-fx-font-size: 16px;");
                                name.setWrapText(true);

                                VBox priceBox = new VBox(2);
                                Label priceLabel = new Label("Giá hiện tại:");
                                priceLabel.getStyleClass().add("muted-text");
                                priceLabel.setStyle("-fx-font-size: 13px;");

                                Label priceValue = new Label(String.format("%,.0f đ", auc.highestBid));
                                priceValue.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 18px;");
                                priceBox.getChildren().addAll(priceLabel, priceValue);

                                Region spacer = new Region();
                                VBox.setVgrow(spacer, Priority.ALWAYS);

                                card.getChildren().addAll(imgView, name, spacer, priceBox);

                                // Click vào Card để xem chi tiết
                                card.setOnMouseClicked(e -> {
                                    try {
                                        // 1. Tải giao diện Chi tiết Đấu giá
                                        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/auction/view/auction/AuctionDetail.fxml"));
                                        javafx.scene.Node view = loader.load();

                                        // 2. Ép kiểu Controller và truyền dữ liệu sản phẩm qua
                                        com.auction.controller.auction.AuctionDetailController controller = loader.getController();
                                        controller.setAuctionData(auc);

                                        // 3. Tìm vùng hiển thị chính (contentArea) và nhét giao diện mới vào
                                        javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) paneItems.getScene().lookup("#contentArea");
                                        if (contentArea != null) {
                                            contentArea.getChildren().setAll(view);
                                        }
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                        System.out.println("Lỗi khi mở chi tiết sản phẩm từ thanh tìm kiếm!");
                                    }
                                });

                                paneItems.getChildren().add(card);
                            }
                        }
                    }
                }
            });
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
                            Label empty = new Label("Không tìm thấy người dùng nào.");
                            empty.getStyleClass().add("muted-text");
                            paneUsers.getChildren().add(empty);
                        } else {
                            for (ConnectionModel.UserModel user : list) {
                                paneUsers.getChildren().add(createUserRow(user));
                            }
                        }
                    }
                }
            });
        });
    }

    private HBox createUserRow(ConnectionModel.UserModel user) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);

        // Nền Card đổi màu tự động theo theme
        row.getStyleClass().add("card");
        row.setStyle("-fx-padding: 15 25;");

        ImageView avt = new ImageView(new Image(ApiService.BASE_URL + user.avatarUrl, true));
        avt.setFitWidth(50); avt.setFitHeight(50);
        Rectangle clip = new Rectangle(50, 50); clip.setArcWidth(50); clip.setArcHeight(50);
        avt.setClip(clip);

        VBox info = new VBox(2);
        info.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label(user.fullName != null ? user.fullName : user.userName);
        name.getStyleClass().add("section-title");
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label username = new Label("@" + user.userName);
        username.getStyleClass().add("muted-text");
        username.setStyle("-fx-font-size: 13px;");
        info.getChildren().addAll(name, username);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnSingleAction = new Button("Đang kiểm tra...");
        btnSingleAction.setDisable(true);
        // Tái sử dụng class btn-primary
        btnSingleAction.getStyleClass().add("btn-primary");
        btnSingleAction.setStyle("-fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20;");

        HBox dualActionBox = new HBox(10);
        Button btnAccept = new Button("✓ Chấp nhận");
        btnAccept.getStyleClass().add("btn-success"); // Chuẩn màu xanh
        btnAccept.setStyle("-fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20;");

        Button btnDecline = new Button("✕ Từ chối");
        btnDecline.getStyleClass().add("btn-danger"); // Chuẩn màu đỏ
        btnDecline.setStyle("-fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20;");

        dualActionBox.getChildren().addAll(btnAccept, btnDecline);
        dualActionBox.setVisible(false);
        dualActionBox.setManaged(false);

        StackPane actionContainer = new StackPane(btnSingleAction, dualActionBox);

        if (user.userName.equals(SessionManager.userName)) {
            actionContainer.setVisible(false);
            actionContainer.setManaged(false);
        } else {
            ApiService.getAsync("/chat/check-connection?user1=" + SessionManager.userName + "&user2=" + user.userName).thenAccept(statusRes -> {
                Platform.runLater(() -> {
                    if (statusRes.statusCode() == 200) {
                        ApiResponse apiRes = ApiService.gson.fromJson(statusRes.body(), ApiResponse.class);

                        if (apiRes.code == 1000 && apiRes.result != null) {
                            String status = String.valueOf(apiRes.result).replaceAll("[^A-Z_]", "");
                            btnSingleAction.getStyleClass().removeAll("btn-success", "btn-danger", "btn-primary");

                            switch (status) {
                                case "ACCEPTED":
                                    btnSingleAction.setVisible(true); btnSingleAction.setManaged(true);
                                    dualActionBox.setVisible(false); dualActionBox.setManaged(false);
                                    btnSingleAction.setText("Đã là bạn bè ✓");
                                    btnSingleAction.getStyleClass().add("btn-success");
                                    break;
                                case "PENDING_SENDER":
                                    btnSingleAction.setVisible(true); btnSingleAction.setManaged(true);
                                    dualActionBox.setVisible(false); dualActionBox.setManaged(false);
                                    btnSingleAction.setText("Đã gửi lời mời");
                                    btnSingleAction.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20;");
                                    break;
                                case "PENDING_RECEIVER":
                                    btnSingleAction.setVisible(false); btnSingleAction.setManaged(false);
                                    dualActionBox.setVisible(true); dualActionBox.setManaged(true);
                                    break;
                                case "NONE":
                                default:
                                    btnSingleAction.setVisible(true); btnSingleAction.setManaged(true);
                                    dualActionBox.setVisible(false); dualActionBox.setManaged(false);
                                    btnSingleAction.setText("➕ Kết bạn");
                                    btnSingleAction.getStyleClass().add("btn-primary");
                                    btnSingleAction.setDisable(false);
                                    break;
                            }
                        }
                    }
                });
            });

            // Gắn sự kiện (giữ nguyên logic gốc của bạn)
            btnSingleAction.setOnAction(e -> { /* Gửi kết bạn... */ });
            btnAccept.setOnAction(e -> { /* Chấp nhận... */ });
            btnDecline.setOnAction(e -> { /* Từ chối... */ });
        }

        row.getChildren().addAll(avt, info, spacer, actionContainer);
        return row;
    }
}
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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.List;

public class SearchController {

    @FXML private Label lblKeyword;
    @FXML private FlowPane paneItems;
    @FXML private VBox paneUsers;

    public void executeSearch(String keyword) {
        lblKeyword.setText("'" + keyword + "'");

        // 1. Tìm Sản phẩm đấu giá
        searchAuctions(keyword);

        // 2. Tìm Người dùng
        searchUsers(keyword);
    }

    private void searchAuctions(String keyword) {
        paneItems.getChildren().clear();

        // FIX LỖI URL: Mã hóa từ khóa (Ví dụ dấu cách " " sẽ thành "%20")
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
                                Label lbl = new Label("📦 " + auc.bidProduct.name + " - Giá: " + String.format("%,.0f đ", auc.highestBid));
                                lbl.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
                                paneItems.getChildren().add(lbl);
                            }
                        }
                    }
                } else {
                    // FIX LỖI TÀNG HÌNH: Nếu API sập thì in ra màn hình
                    paneItems.getChildren().add(new Label("Lỗi máy chủ khi tìm sản phẩm: " + res.statusCode()));
                    System.err.println("Search Auction Error: " + res.body());
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> paneItems.getChildren().add(new Label("Mất kết nối tới máy chủ!")));
            return null;
        });
    }

    private void searchUsers(String keyword) {
        paneUsers.getChildren().clear();

        // FIX LỖI URL: Mã hóa từ khóa
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
                    // FIX LỖI TÀNG HÌNH: Nếu API sập thì in ra màn hình
                    paneUsers.getChildren().add(new Label("Lỗi máy chủ khi tìm người dùng: " + res.statusCode()));
                    System.err.println("Search User Error: " + res.body());
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
        row.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");

        ImageView avt = new ImageView(new Image(ApiService.BASE_URL + user.avatarUrl, true));
        avt.setFitWidth(50); avt.setFitHeight(50);
        Rectangle clip = new Rectangle(50, 50); clip.setArcWidth(50); clip.setArcHeight(50);
        avt.setClip(clip);

        VBox info = new VBox(2);
        Label name = new Label(user.fullName != null ? user.fullName : user.userName);
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");
        Label username = new Label("@" + user.userName);
        username.setStyle("-fx-text-fill: #7f8c8d;");
        info.getChildren().addAll(name, username);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Nút Gửi kết bạn mặc định
        Button btnAddFriend = new Button("Đang kiểm tra...");
        btnAddFriend.setDisable(true); // Khóa tạm trong lúc chờ API phản hồi
        btnAddFriend.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20;");

        // Ẩn nút nếu đang tìm chính mình
        if (user.userName.equals(SessionManager.userName)) {
            btnAddFriend.setVisible(false);
            btnAddFriend.setManaged(false);
        } else {
            // ========================================================
            // GỌI API KIỂM TRA TRẠNG THÁI ĐỂ RENDER GIAO DIỆN NÚT BẤM
            // ========================================================
            ApiService.getAsync("/chat/check-connection?user1=" + SessionManager.userName + "&user2=" + user.userName).thenAccept(statusRes -> {
                Platform.runLater(() -> {
                    if (statusRes.statusCode() == 200) {
                        ApiResponse apiRes = ApiService.gson.fromJson(statusRes.body(), ApiResponse.class);
                        String status = apiRes.result.toString();

                        switch (status) {
                            case "ACCEPTED":
                                btnAddFriend.setText("Đã là bạn bè ✓");
                                btnAddFriend.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20;");
                                btnAddFriend.setDisable(true);
                                break;
                            case "PENDING_SENDER":
                                btnAddFriend.setText("Đã gửi lời mời");
                                btnAddFriend.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20;");
                                btnAddFriend.setDisable(true);
                                break;
                            case "PENDING_RECEIVER":
                                btnAddFriend.setText("Vào hòm thư để X.nhận");
                                btnAddFriend.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20;");
                                btnAddFriend.setDisable(true);
                                break;
                            case "NONE":
                            default:
                                btnAddFriend.setText("➕ Kết bạn");
                                btnAddFriend.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;");
                                btnAddFriend.setDisable(false);
                                break;
                        }
                    }
                });
            });

            // Lập trình sự kiện cho nút (Chỉ bấm được khi ở trạng thái NONE)
            btnAddFriend.setOnAction(e -> {
                btnAddFriend.setText("Đang gửi...");
                btnAddFriend.setDisable(true);

                ApiService.postAsync("/chat/friend-request?sender=" + SessionManager.userName + "&receiver=" + user.userName, null).thenAccept(res -> {
                    Platform.runLater(() -> {
                        if (res.statusCode() >= 200 && res.statusCode() < 300) {
                            btnAddFriend.setText("Đã gửi lời mời");
                            btnAddFriend.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20;");
                        } else {
                            // FIX LỖI POPUP KHÔNG HIỆN RÕ NGUYÊN NHÂN
                            btnAddFriend.setText("➕ Kết bạn");
                            btnAddFriend.setDisable(false);
                            try {
                                ApiResponse err = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                                new Alert(Alert.AlertType.ERROR, err.message != null ? err.message : "Lỗi hệ thống").show();
                            } catch (Exception ex) {
                                new Alert(Alert.AlertType.ERROR, "Lỗi kết nối máy chủ").show();
                            }
                        }
                    });
                });
            });
        }

        row.getChildren().addAll(avt, info, spacer, btnAddFriend);
        return row;
    }
}
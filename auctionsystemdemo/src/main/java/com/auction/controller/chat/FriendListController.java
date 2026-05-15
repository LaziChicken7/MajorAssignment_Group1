package com.auction.controller.chat;

import com.auction.model.ApiResponse;
import com.auction.model.ConnectionModel;
import com.auction.model.NotificationModel;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

public class FriendListController {

    @FXML private VBox vboxPending;
    @FXML private VBox vboxFriends;

    @FXML
    public void initialize() {
        loadPendingRequests();
        loadFriends();
    }

    // ============================================
    // NÚT BACK VỀ LẠI TRANG CHAT
    // ============================================
    @FXML
    public void goBack(javafx.scene.input.MouseEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/auction/view/chat/Chat.fxml"));
            javafx.scene.Node view = loader.load();
            javafx.scene.Node source = (javafx.scene.Node) event.getSource();
            javafx.scene.layout.Pane contentArea = (javafx.scene.layout.Pane) source.getScene().lookup("#contentArea");

            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Nút Back dành cho ActionEvent (Nút bấm thông thường)
    private void openChatPage(javafx.event.ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/auction/view/chat/Chat.fxml"));
            javafx.scene.Node view = loader.load();
            javafx.scene.Node source = (javafx.scene.Node) event.getSource();
            javafx.scene.layout.Pane contentArea = (javafx.scene.layout.Pane) source.getScene().lookup("#contentArea");

            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============================================
    // LOAD DANH SÁCH PENDING TỪ API THÔNG BÁO
    // ============================================
    private void loadPendingRequests() {
        vboxPending.getChildren().clear();
        ApiService.getAsync("/notifications/" + SessionManager.userName).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    Type listType = new TypeToken<List<NotificationModel>>(){}.getType();
                    List<NotificationModel> notifications = ApiService.gson.fromJson(apiRes.result, listType);

                    // Lọc ra các thông báo là Lời mời kết bạn
                    List<NotificationModel> pendingRequests = notifications.stream()
                            .filter(n -> "FRIEND_REQUEST".equals(n.type))
                            .collect(Collectors.toList());

                    if (pendingRequests.isEmpty()) {
                        Label lbl = new Label("Không có lời mời kết bạn nào.");
                        lbl.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic;");
                        vboxPending.getChildren().add(lbl);
                    } else {
                        for (NotificationModel notif : pendingRequests) {
                            // Cắt chuỗi lấy Username từ Title: "Yêu cầu kết bạn từ: admin" -> "admin"
                            String senderUsername = notif.title.replace("Yêu cầu kết bạn từ: ", "").trim();
                            vboxPending.getChildren().add(createPendingItem(senderUsername, notif));
                        }
                    }
                }
            });
        });
    }

    // ============================================
    // LOAD DANH SÁCH BẠN BÈ TỪ API FRIENDS
    // ============================================
    private void loadFriends() {
        vboxFriends.getChildren().clear();
        ApiService.getAsync("/chat/friends?username=" + SessionManager.userName).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    Type listType = new TypeToken<List<ConnectionModel>>(){}.getType();
                    List<ConnectionModel> friends = ApiService.gson.fromJson(apiRes.result, listType);

                    if (friends == null || friends.isEmpty()) {
                        Label lbl = new Label("Bạn chưa có người bạn nào.");
                        lbl.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic;");
                        vboxFriends.getChildren().add(lbl);
                    } else {
                        for (ConnectionModel conn : friends) {
                            ConnectionModel.UserModel friend = conn.sender.userName.equals(SessionManager.userName) ? conn.receiver : conn.sender;
                            vboxFriends.getChildren().add(createFriendItem(friend));
                        }
                    }
                }
            });
        });
    }

    // ============================================
    // VẼ KHUNG LỜI MỜI KẾT BẠN (CÓ CHỌN ĐỒNG Ý/TỪ CHỐI)
    // ============================================
    private HBox createPendingItem(String senderUsername, NotificationModel notif) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");

        // Avatar mặc định chờ tải
        ImageView avt = new ImageView(new Image(ApiService.BASE_URL + "/uploads/images/avatar/avatarmacdinh.png", true));
        avt.setFitWidth(50); avt.setFitHeight(50);
        Rectangle clip = new Rectangle(50, 50); clip.setArcWidth(50); clip.setArcHeight(50);
        avt.setClip(clip);

        VBox info = new VBox(3);
        Label name = new Label("@" + senderUsername);
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");
        Label desc = new Label(notif.description);
        desc.setStyle("-fx-text-fill: #7f8c8d;");
        info.getChildren().addAll(name, desc);

        // Fetch API phụ để lấy đúng Tên thật và Ảnh đại diện của người gửi
        ApiService.getAsync("/users/profile/" + senderUsername).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    try {
                        ApiResponse profileRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        ConnectionModel.UserModel userProfile = ApiService.gson.fromJson(profileRes.result, ConnectionModel.UserModel.class);
                        name.setText(userProfile.fullName != null ? userProfile.fullName : userProfile.userName);
                        avt.setImage(new Image(ApiService.BASE_URL + userProfile.avatarUrl, true));
                    } catch (Exception ignored) {}
                }
            });
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAccept = new Button("✔ Chấp nhận");
        btnAccept.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");
        btnAccept.setOnAction(e -> {
            btnAccept.setText("Đang xử lý...");
            ApiService.putAsync("/notifications/" + notif.notificationId + "/accept", null).thenAccept(res -> {
                Platform.runLater(() -> {
                    loadPendingRequests(); // Xóa khỏi danh sách chờ
                    loadFriends();         // Bỏ người đó vào danh sách bạn bè
                });
            });
        });

        Button btnDecline = new Button("✖ Từ chối");
        btnDecline.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");
        btnDecline.setOnAction(e -> {
            btnDecline.setText("Đang xử lý...");
            ApiService.putAsync("/notifications/" + notif.notificationId + "/decline", null).thenAccept(res -> {
                Platform.runLater(() -> loadPendingRequests()); // Xóa khỏi danh sách chờ
            });
        });

        row.getChildren().addAll(avt, info, spacer, btnAccept, btnDecline);
        return row;
    }

    // ============================================
    // VẼ KHUNG BẠN BÈ (CÓ NÚT NHẮN TIN)
    // ============================================
    private HBox createFriendItem(ConnectionModel.UserModel friend) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");

        ImageView avt = new ImageView(new Image(ApiService.BASE_URL + friend.avatarUrl, true));
        avt.setFitWidth(50); avt.setFitHeight(50);
        Rectangle clip = new Rectangle(50, 50); clip.setArcWidth(50); clip.setArcHeight(50);
        avt.setClip(clip);

        VBox info = new VBox(3);
        Label name = new Label(friend.fullName != null ? friend.fullName : friend.userName);
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");
        Label username = new Label("@" + friend.userName);
        username.setStyle("-fx-text-fill: #7f8c8d;");
        info.getChildren().addAll(name, username);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnChat = new Button("💬 Nhắn tin");
        btnChat.setStyle("-fx-background-color: #3b5998; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");
        btnChat.setOnAction(this::openChatPage); // Bấm vào sẽ quay về trang nhắn tin

        row.getChildren().addAll(avt, info, spacer, btnChat);
        return row;
    }
}
package com.auction.controller.chat;


import lombok.extern.slf4j.Slf4j;
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

@Slf4j
public class FriendListController {

    @FXML private VBox vboxPending;
    @FXML private VBox vboxFriends;

    @FXML
    public void initialize() {
        log.info("\u25B6 Controller Action - Execute: initialize()");
        loadPendingRequests();
        loadFriends();
    }

    // ============================================
    // NÚT BACK VỀ LẠI TRANG CHAT
    // ============================================
    @FXML
    public void goBack(javafx.scene.input.MouseEvent event) {
        log.info("\u25B6 Controller Action - Execute: goBack()");
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/auction/view/chat/Chat.fxml"));
            javafx.scene.Node view = loader.load();
            javafx.scene.Node source = (javafx.scene.Node) event.getSource();
            javafx.scene.layout.Pane contentArea = (javafx.scene.layout.Pane) source.getScene().lookup("#contentArea");

            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            log.error("Exception occurred", e);
        }
    }

    // Nút Back dành cho ActionEvent (Nút bấm thông thường)
    private void openChatPage(javafx.event.ActionEvent event) {
        log.info("\u25B6 Controller Action - Execute: openChatPage()");
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/auction/view/chat/Chat.fxml"));
            javafx.scene.Node view = loader.load();
            javafx.scene.Node source = (javafx.scene.Node) event.getSource();
            javafx.scene.layout.Pane contentArea = (javafx.scene.layout.Pane) source.getScene().lookup("#contentArea");

            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            log.error("Exception occurred", e);
        }
    }

    // ============================================
    // LOAD DANH SÁCH PENDING TỪ API THÔNG BÁO
    // ============================================
    private void loadPendingRequests() {
        log.info("\u25B6 Controller Action - Execute: loadPendingRequests()");
        vboxPending.getChildren().clear();
        ApiService.getAsync("/notifications/" + SessionManager.userName).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    Type listType = new TypeToken<List<NotificationModel>>(){}.getType();
                    List<NotificationModel> notifications = ApiService.gson.fromJson(apiRes.result, listType);
                    List<NotificationModel> pendingRequests = notifications.stream().filter(n -> "FRIEND_REQUEST".equals(n.type)).collect(Collectors.toList());
                    if (pendingRequests.isEmpty()) {
                        Label lbl = new Label("Không có lời mời kết bạn nào.");
                        lbl.getStyleClass().add("friend-empty-text"); // Gọi CSS class
                        vboxPending.getChildren().add(lbl);
                    } else {
                        for (NotificationModel notif : pendingRequests) {
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
        log.info("\u25B6 Controller Action - Execute: loadFriends()");
        vboxFriends.getChildren().clear();
        ApiService.getAsync("/chat/friends?username=" + SessionManager.userName).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    Type listType = new TypeToken<List<ConnectionModel>>(){}.getType();
                    List<ConnectionModel> friends = ApiService.gson.fromJson(apiRes.result, listType);

                    if (friends == null || friends.isEmpty()) {
                        Label lbl = new Label("Bạn chưa có người bạn nào.");
                        lbl.getStyleClass().add("friend-empty-text"); // Gọi CSS class
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
        row.getStyleClass().add("friend-item-card");

        ImageView avt = new ImageView(new Image(ApiService.BASE_URL + "/uploads/images/avatar/avatarmacdinh.png", true));
        avt.setFitWidth(50); avt.setFitHeight(50);
        Rectangle clip = new Rectangle(50, 50); clip.setArcWidth(50); clip.setArcHeight(50);
        avt.setClip(clip);

        VBox info = new VBox(3);
        Label name = new Label("@" + senderUsername);
        name.getStyleClass().add("friend-item-name");
        Label desc = new Label(notif.description);
        desc.getStyleClass().add("friend-item-desc");
        info.getChildren().addAll(name, desc);

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
        btnAccept.getStyleClass().add("friend-btn-accept");
        btnAccept.setOnAction(e -> {
            btnAccept.setText("Đang xử lý...");
            ApiService.putAsync("/notifications/" + notif.notificationId + "/accept", null).thenAccept(res -> {
                Platform.runLater(() -> {
                    loadPendingRequests();
                    loadFriends();
                });
            });
        });

        Button btnDecline = new Button("✖ Từ chối");
        btnDecline.getStyleClass().add("friend-btn-decline");
        btnDecline.setOnAction(e -> {
            btnDecline.setText("Đang xử lý...");
            ApiService.putAsync("/notifications/" + notif.notificationId + "/decline", null).thenAccept(res -> {
                Platform.runLater(() -> loadPendingRequests());
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
        row.getStyleClass().add("friend-item-card");

        ImageView avt = new ImageView(new Image(ApiService.BASE_URL + friend.avatarUrl, true));
        avt.setFitWidth(50); avt.setFitHeight(50);
        Rectangle clip = new Rectangle(50, 50); clip.setArcWidth(50); clip.setArcHeight(50);
        avt.setClip(clip);

        VBox info = new VBox(3);
        Label name = new Label(friend.fullName != null ? friend.fullName : friend.userName);
        name.getStyleClass().add("friend-item-name");
        Label username = new Label("@" + friend.userName);
        username.getStyleClass().add("friend-item-desc");
        info.getChildren().addAll(name, username);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnChat = new Button("💬 Nhắn tin");
        btnChat.getStyleClass().add("friend-btn-chat");
        btnChat.setOnAction(this::openChatPage);

        row.getChildren().addAll(avt, info, spacer, btnChat);
        return row;
    }
}
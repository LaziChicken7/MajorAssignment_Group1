package com.auction.controller.chat;

import com.auction.model.ApiResponse;
import com.auction.model.ChatMessageModel;
import com.auction.model.ConnectionModel;
import com.auction.util.ApiService;
import com.auction.util.GlobalWebSocketManager;
import com.auction.util.SessionManager;
import com.auction.util.WebSocketClientService;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Popup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ChatController {

    @FXML private VBox vboxFriends, vboxMessages, paneWaiting;
    @FXML private HBox chatHeader, paneInput;
    @FXML private Label lblChatName;
    @FXML private ImageView imgChatAvatar;
    @FXML private TextField txtMessage;
    @FXML private ScrollPane scrollMessages;
    @FXML private TextField txtSearchFriend;
    @FXML private Button btnAttachment;
    @FXML private Button btnInfo;

    // ... Các biến cũ giữ nguyên
    @FXML private VBox paneFriendList, paneChatInfo, paneMainChat;
    @FXML private SVGPath iconOnlineStatus;
    @FXML private Label lblOnlineStatus;

    @FXML private ImageView imgInfoAvatar;
    @FXML private Label lblInfoName, lblInfoRole;

    private boolean isInfoPanelOpen = false; // Biến theo dõi trạng thái cột Info// Khai báo nút Info

    private List<ConnectionModel> allFriendsList = new ArrayList<>();
    private WebSocketClientService webSocketService;
    private String currentChatPartner = null;
    private Popup attachmentMenuPopup;

    // =======================================================
    // TÍNH NĂNG MỚI: THEO DÕI GHIM VÀ CHƯA ĐỌC
    // =======================================================
    private Set<String> pinnedUsers = new HashSet<>();
    private Set<String> unreadUsers = new HashSet<>();

    @FXML
    public void initialize() {
        vboxMessages.heightProperty().addListener((observable, oldValue, newValue) -> scrollMessages.setVvalue(1.0));
        loadFriendsList();
        GlobalWebSocketManager.setActiveChatListener(this::handleIncomingMessageUI);

        vboxMessages.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                GlobalWebSocketManager.currentActiveChatPartner = null;
                GlobalWebSocketManager.setActiveChatListener(null);
            }
        });
    }

    @FXML
    public void goBack(javafx.scene.input.MouseEvent event) {
        try {
            GlobalWebSocketManager.currentActiveChatPartner = null;
            GlobalWebSocketManager.setActiveChatListener(null);
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/auction/view/home/Home.fxml"));
            javafx.scene.Node view = loader.load();
            javafx.scene.Node source = (javafx.scene.Node) event.getSource();
            javafx.scene.layout.Pane contentArea = (javafx.scene.layout.Pane) source.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleIncomingMessageUI(ChatMessageModel msg) {
        if (msg == null) return;
        processNewMessageState(msg);
    }

    private void handleIncomingMessage(String jsonPayload) {
        Platform.runLater(() -> {
            try {
                String cleanJson = jsonPayload.trim();
                ChatMessageModel msg = null;

                if (cleanJson.startsWith("[")) {
                    java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<ChatMessageModel>>(){}.getType();
                    java.util.List<ChatMessageModel> list = ApiService.gson.fromJson(cleanJson, listType);
                    if (list != null && !list.isEmpty()) msg = list.get(0);
                } else {
                    msg = ApiService.gson.fromJson(cleanJson, ChatMessageModel.class);
                }

                if (msg != null) processNewMessageState(msg);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // =======================================================
    // XỬ LÝ TIN NHẮN ĐẾN: ĐẨY LÊN ĐẦU & ĐÁNH DẤU CHƯA ĐỌC
    // =======================================================
    private void processNewMessageState(ChatMessageModel msg) {
        String senderName = msg.sender.userName;
        boolean isRelevantChat =
                (senderName.equals(SessionManager.userName) && msg.receiver.userName.equals(currentChatPartner)) ||
                        (senderName.equals(currentChatPartner) && msg.receiver.userName.equals(SessionManager.userName));

        if (isRelevantChat) {
            // Nếu đang mở đoạn chat đó -> In ra luôn
            boolean isMe = senderName.equals(SessionManager.userName);
            vboxMessages.getChildren().add(createChatBubble(msg.content, isMe));
        } else {
            // Nếu là tin nhắn từ người khác (mình không mở) -> Đánh dấu chấm đỏ chưa đọc
            if (!senderName.equals(SessionManager.userName)) {
                unreadUsers.add(senderName);
            }
        }

        // Kéo người dùng có tin nhắn mới lên đầu danh sách gốc
        String targetUser = senderName.equals(SessionManager.userName) ? msg.receiver.userName : senderName;
        ConnectionModel targetConn = null;
        for (ConnectionModel conn : allFriendsList) {
            ConnectionModel.UserModel friend = conn.sender.userName.equals(SessionManager.userName) ? conn.receiver : conn.sender;
            if (friend.userName.equals(targetUser)) {
                targetConn = conn;
                break;
            }
        }

        if (targetConn != null) {
            allFriendsList.remove(targetConn);
            allFriendsList.add(0, targetConn); // Cắm lên đầu List
            // Nếu không đang dùng ô tìm kiếm, render lại UI ngay lập tức
            if (txtSearchFriend.getText().trim().isEmpty()) {
                renderFriendsList(allFriendsList);
            }
        }
    }

    private void loadFriendsList() {
        ApiService.getAsync("/chat/friends?username=" + SessionManager.userName).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        java.lang.reflect.Type listType = new TypeToken<List<ConnectionModel>>(){}.getType();
                        allFriendsList = ApiService.gson.fromJson(apiRes.result, listType);
                        renderFriendsList(allFriendsList);
                        setupSearchFeature();
                    }
                }
            });
        });
    }

    // =======================================================
    // RENDER BẠN BÈ: TỰ ĐỘNG SẮP XẾP GHIM & CHƯA ĐỌC LÊN TRÊN
    // =======================================================
    private void renderFriendsList(List<ConnectionModel> listToRender) {
        vboxFriends.getChildren().clear();
        if (listToRender == null || listToRender.isEmpty()) {
            Label lblEmpty = new Label("Không tìm thấy bạn bè nào.");
            lblEmpty.setStyle("-fx-padding: 10; -fx-text-fill: #7f8c8d;");
            vboxFriends.getChildren().add(lblEmpty);
            return;
        }

        // Thuật toán sắp xếp trước khi vẽ (Ghim -> Chưa đọc -> Thứ tự tin nhắn mới nhất)
        List<ConnectionModel> sortedList = new ArrayList<>(listToRender);
        sortedList.sort((conn1, conn2) -> {
            ConnectionModel.UserModel f1 = conn1.sender.userName.equals(SessionManager.userName) ? conn1.receiver : conn1.sender;
            ConnectionModel.UserModel f2 = conn2.sender.userName.equals(SessionManager.userName) ? conn2.receiver : conn2.sender;

            boolean p1 = pinnedUsers.contains(f1.userName);
            boolean p2 = pinnedUsers.contains(f2.userName);
            if (p1 != p2) return p1 ? -1 : 1; // Pinned ưu tiên cao nhất

            boolean u1 = unreadUsers.contains(f1.userName);
            boolean u2 = unreadUsers.contains(f2.userName);
            if (u1 != u2) return u1 ? -1 : 1; // Chưa đọc ưu tiên số 2

            return 0; // Giữ nguyên vị trí ban đầu (Tin nhắn mới nhất)
        });

        for (ConnectionModel conn : sortedList) {
            ConnectionModel.UserModel friend = conn.sender.userName.equals(SessionManager.userName) ? conn.receiver : conn.sender;
            vboxFriends.getChildren().add(createFriendItem(friend));
        }
    }

    private void setupSearchFeature() {
        txtSearchFriend.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                renderFriendsList(allFriendsList);
            } else {
                String keyword = newValue.toLowerCase().trim();
                List<ConnectionModel> filteredList = allFriendsList.stream().filter(conn -> {
                    ConnectionModel.UserModel friend = conn.sender.userName.equals(SessionManager.userName) ? conn.receiver : conn.sender;
                    String friendName = friend.fullName != null ? friend.fullName : friend.userName;
                    return friendName.toLowerCase().contains(keyword);
                }).collect(Collectors.toList());
                renderFriendsList(filteredList);
            }
        });
    }

    // =======================================================
    // TẠO ITEM BẠN BÈ: CÓ MENU CHUỘT PHẢI ĐỂ GHIM, CHẤM ĐỎ CHƯA ĐỌC
    // =======================================================
    private HBox createFriendItem(ConnectionModel.UserModel friend) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("friend-chat-item");

        if (currentChatPartner != null && currentChatPartner.equals(friend.userName)) {
            item.getStyleClass().add("active-chat");
        }

        ImageView avt = new ImageView(new Image(ApiService.BASE_URL + friend.avatarUrl, true));
        avt.setFitWidth(45); avt.setFitHeight(45);
        Rectangle clip = new Rectangle(45, 45); clip.setArcWidth(45); clip.setArcHeight(45);
        avt.setClip(clip);

        Label name = new Label(friend.fullName != null ? friend.fullName : friend.userName);
        name.getStyleClass().add("row-title-bold");
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");

        // UI: Icon Ghim (Màu vàng)
        SVGPath pinIcon = new SVGPath();
        pinIcon.setContent("M 16 12 V 4 h 1 V 2 H 7 v 2 h 1 v 8 l -2 2 v 2 h 5.2 v 6 h 1.6 v -6 H 18 v -2 l -2 -2 z");
        pinIcon.setFill(Color.web("#f39c12"));
        pinIcon.setScaleX(0.7); pinIcon.setScaleY(0.7);
        pinIcon.setVisible(pinnedUsers.contains(friend.userName));
        pinIcon.setManaged(pinnedUsers.contains(friend.userName));

        // UI: Chấm đỏ chưa đọc
        Circle unreadDot = new Circle(5, Color.web("#e74c3c"));
        unreadDot.setVisible(unreadUsers.contains(friend.userName));
        unreadDot.setManaged(unreadUsers.contains(friend.userName));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        item.getChildren().addAll(avt, name, spacer, pinIcon, unreadDot);

        // Chuột phải (ContextMenu) để Ghim/Bỏ ghim
        ContextMenu contextMenu = new ContextMenu();
        MenuItem pinItem = new MenuItem(pinnedUsers.contains(friend.userName) ? "Bỏ ghim hội thoại" : "📌 Ghim hội thoại");
        pinItem.setOnAction(e -> {
            if (pinnedUsers.contains(friend.userName)) {
                pinnedUsers.remove(friend.userName);
            } else {
                pinnedUsers.add(friend.userName);
            }
            renderFriendsList(allFriendsList); // Sắp xếp lại ngay
        });
        contextMenu.getItems().add(pinItem);

        item.setOnContextMenuRequested(e -> contextMenu.show(item, e.getScreenX(), e.getScreenY()));

        // Chuột trái để Chat
        item.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                // Xóa trạng thái chưa đọc
                if (unreadUsers.contains(friend.userName)) {
                    unreadUsers.remove(friend.userName);
                    renderFriendsList(allFriendsList);
                } else {
                    for (javafx.scene.Node node : vboxFriends.getChildren()) {
                        node.getStyleClass().remove("active-chat");
                    }
                    item.getStyleClass().add("active-chat");
                }
                openChatWith(friend);
            }
        });

        return item;
    }

    private void openChatWith(ConnectionModel.UserModel friend) {
        currentChatPartner = friend.userName;
        GlobalWebSocketManager.currentActiveChatPartner = currentChatPartner;

        lblChatName.setText(friend.fullName != null ? friend.fullName : friend.userName);
        imgChatAvatar.setImage(new Image(ApiService.BASE_URL + friend.avatarUrl, true));
        Rectangle clip = new Rectangle(45, 45); clip.setArcWidth(45); clip.setArcHeight(45);
        imgChatAvatar.setClip(clip);

        paneWaiting.setVisible(false); paneWaiting.setManaged(false);
        chatHeader.setVisible(true); chatHeader.setManaged(true);
        scrollMessages.setVisible(true); scrollMessages.setManaged(true);
        paneInput.setVisible(true); paneInput.setManaged(true);

        loadChatHistory();

        // KIỂM TRA TRẠNG THÁI ONLINE
        checkOnlineStatus(friend.userName);

        // NẾU CỘT THÔNG TIN BÊN PHẢI ĐANG MỞ, THÌ LOAD LẠI DATA LUÔN
        if (isInfoPanelOpen) {
            updateInfoPanelData(friend);
        }
    }

    // =======================================================
    // TÍNH NĂNG MỚI: HIỂN THỊ THÔNG TIN KHI BẤM NÚT CHỮ "i"
    // =======================================================
    @FXML
    private void showChatInfo() {
        if (currentChatPartner == null) return;

        // Đảo ngược trạng thái
        isInfoPanelOpen = !isInfoPanelOpen;

        // Nếu mở Info -> Tắt List bạn bè đi, Hiện Info lên
        paneFriendList.setVisible(!isInfoPanelOpen);
        paneFriendList.setManaged(!isInfoPanelOpen);

        paneChatInfo.setVisible(isInfoPanelOpen);
        paneChatInfo.setManaged(isInfoPanelOpen);

        // Đổ dữ liệu vào bảng Info nếu nó đang mở
        if (isInfoPanelOpen) {
            // Tìm thông tin người đang chat
            ConnectionModel.UserModel partner = null;
            for (ConnectionModel conn : allFriendsList) {
                ConnectionModel.UserModel friend = conn.sender.userName.equals(SessionManager.userName) ? conn.receiver : conn.sender;
                if (friend.userName.equals(currentChatPartner)) {
                    partner = friend;
                    break;
                }
            }
            if (partner != null) updateInfoPanelData(partner);
        }
    }

    private void updateInfoPanelData(ConnectionModel.UserModel partner) {
        lblInfoName.setText(partner.fullName != null ? partner.fullName : partner.userName);
        imgInfoAvatar.setImage(new Image(ApiService.BASE_URL + partner.avatarUrl, true));

        // Bo tròn Avatar bên cột Info
        Rectangle clip = new Rectangle(100, 100);
        clip.setArcWidth(100); clip.setArcHeight(100);
        imgInfoAvatar.setClip(clip);

        lblInfoRole.setText("Đang tải...");

        // Gọi API lấy Role chính xác (Giống cách cũ, nhưng đổ chữ vào Label thay vì hiện Alert)
        ApiService.getAsync("/users/" + partner.userName).thenAccept(res -> {
            Platform.runLater(() -> {
                String roleDisplay = "Thành viên (User)";
                if (res.statusCode() >= 200 && res.statusCode() < 300) {
                    try {
                        ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        if (apiRes.code == 1000) {
                            com.auction.model.UserModel fullUser = ApiService.gson.fromJson(apiRes.result, com.auction.model.UserModel.class);
                            if ("SELLER".equals(fullUser.role)) roleDisplay = "Người bán (Seller)";
                            else if ("ADMIN".equals(fullUser.role)) roleDisplay = "Quản trị viên (Admin)";
                        }
                    } catch (Exception ignored) {}
                }
                lblInfoRole.setText(roleDisplay);
            });
        });
    }

    private void loadChatHistory() {
        vboxMessages.getChildren().clear();
        ApiService.getAsync("/chat/history?user1=" + SessionManager.userName + "&user2=" + currentChatPartner).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        java.lang.reflect.Type listType = new TypeToken<List<ChatMessageModel>>(){}.getType();
                        List<ChatMessageModel> history = ApiService.gson.fromJson(apiRes.result, listType);

                        for (ChatMessageModel msg : history) {
                            boolean isMe = msg.sender.userName.equals(SessionManager.userName);
                            vboxMessages.getChildren().add(createChatBubble(msg.content, isMe));
                        }
                    }
                }
            });
        });
    }

    private HBox createChatBubble(String text, boolean isMe) {
        HBox row = new HBox();
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(400);

        if (isMe) {
            row.setAlignment(Pos.CENTER_RIGHT);
            bubble.getStyleClass().add("chat-bubble-me");
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            bubble.getStyleClass().add("chat-bubble-other");
        }
        row.getChildren().add(bubble);
        return row;
    }

    @FXML
    private void sendMessage() {
        String text = txtMessage.getText().trim();
        if (text.isEmpty() || currentChatPartner == null) return;
        GlobalWebSocketManager.sendMessage(SessionManager.userName, currentChatPartner, text);

        // Đẩy chính mình lên đầu khi mình vừa gửi tin
        ConnectionModel targetConn = null;
        for (ConnectionModel conn : allFriendsList) {
            ConnectionModel.UserModel friend = conn.sender.userName.equals(SessionManager.userName) ? conn.receiver : conn.sender;
            if (friend.userName.equals(currentChatPartner)) {
                targetConn = conn;
                break;
            }
        }
        if (targetConn != null) {
            allFriendsList.remove(targetConn);
            allFriendsList.add(0, targetConn);
            if (txtSearchFriend.getText().trim().isEmpty()) renderFriendsList(allFriendsList);
        }

        txtMessage.clear();
    }

    @FXML
    private void showAttachmentMenu() {
        if (attachmentMenuPopup == null) {
            attachmentMenuPopup = new Popup();
            attachmentMenuPopup.setAutoHide(true);

            VBox menuBox = new VBox();
            menuBox.getStyleClass().add("card");
            menuBox.setStyle("-fx-padding: 5; -fx-background-radius: 8; -fx-border-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 3);");

            if (btnAttachment.getScene() != null) {
                menuBox.getStylesheets().addAll(btnAttachment.getScene().getStylesheets());
                if (btnAttachment.getScene().getRoot().getStyleClass().contains("dark-theme")) {
                    menuBox.getStyleClass().add("dark-theme");
                }
            }

            String imgSvg = "M 21 19 V 5 c 0 -1.1 -.9 -2 -2 -2 H 5 c -1.1 0 -2 .9 -2 2 v 14 c 0 1.1 .9 2 2 2 h 14 c 1.1 0 2 -.9 2 -2 z M 8.5 13.5 l 2.5 3.01 L 14.5 12 l 4.5 6 H 5 l 3.5 -4.5 z";
            String clipSvg = "M 16.5 6 v 11.5 c 0 2.21 -1.79 4 -4 4 s -4 -1.79 -4 -4 V 5 a 2.5 2.5 0 0 1 5 0 v 10.5 c 0 .55 -.45 1 -1 1 s -1 -.45 -1 -1 V 6 H 10 v 9.5 a 2.5 2.5 0 0 0 5 0 V 5 c 0 -2.21 -1.79 -4 -4 -4 S 7 2.79 7 5 v 12.5 c 0 3.04 2.46 5.5 5.5 -5.5 s 5.5 -2.46 5.5 -5.5 V 6 h -1.5 z";

            HBox btnImage = createPopupMenuItem("Thêm ảnh", imgSvg, () -> attachmentMenuPopup.hide());
            HBox btnFile = createPopupMenuItem("Thêm đính kèm", clipSvg, () -> attachmentMenuPopup.hide());

            menuBox.getChildren().addAll(btnImage, btnFile);
            attachmentMenuPopup.getContent().add(menuBox);
        }

        javafx.geometry.Bounds bounds = btnAttachment.localToScreen(btnAttachment.getBoundsInLocal());
        attachmentMenuPopup.show(btnAttachment, bounds.getMinX() - 10, bounds.getMinY() - 105);
    }

    private HBox createPopupMenuItem(String text, String svgPath, Runnable action) {
        SVGPath icon = new SVGPath();
        icon.setContent(svgPath);
        icon.getStyleClass().add("chat-attachment-icon");
        icon.setScaleX(0.8); icon.setScaleY(0.8);

        Label label = new Label(text);
        label.getStyleClass().add("chat-attachment-label");

        HBox box = new HBox(15, icon, label);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("chat-attachment-item");
        box.setOnMouseClicked(e -> action.run());

        return box;
    }

    @FXML
    private void showAllFriends(javafx.event.ActionEvent event) {
        try {
            GlobalWebSocketManager.currentActiveChatPartner = null;
            GlobalWebSocketManager.setActiveChatListener(null);
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/auction/view/chat/FriendList.fxml"));
            javafx.scene.Node view = loader.load();
            javafx.scene.Node source = (javafx.scene.Node) event.getSource();
            javafx.scene.layout.Pane contentArea = (javafx.scene.layout.Pane) source.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkOnlineStatus(String username) {
        // Mặc định cho hiển thị Ngoại tuyến (Màu xám) trong lúc chờ Server
        setOnlineStatusUI(false);

        // Gọi API lên Server kiểm tra (Ví dụ đường dẫn API là /users/{username}/status)
        ApiService.getAsync("/users/" + username + "/status").thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    // Giả sử API trả về chuỗi "ONLINE" hoặc "true"
                    boolean isOnline = res.body().contains("true") || res.body().toUpperCase().contains("ONLINE");
                    setOnlineStatusUI(isOnline);
                }
            });
        }).exceptionally(ex -> {
            return null; // Im lặng nếu API chưa được làm ở Backend
        });
    }

    private void setOnlineStatusUI(boolean isOnline) {
        if (isOnline) {
            iconOnlineStatus.setFill(Color.web("#2ecc71")); // Xanh lá mạ
            lblOnlineStatus.setText("Đang hoạt động");
        } else {
            iconOnlineStatus.setFill(Color.web("#95a5a6")); // Màu Xám
            lblOnlineStatus.setText("Ngoại tuyến");
        }
    }
}
package com.auction.controller.chat;

import com.auction.model.ApiResponse;
import com.auction.model.ChatMessageModel;
import com.auction.model.ConnectionModel;
import com.auction.util.ApiService;
import com.auction.util.GlobalWebSocketManager;
import com.auction.util.SessionManager;
import com.auction.util.WebSocketClientService;
import com.google.gson.reflect.TypeToken;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
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
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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

    @FXML private VBox paneFriendList, paneChatInfo, paneMainChat;
    @FXML private SVGPath iconOnlineStatus;
    @FXML private Label lblOnlineStatus;

    @FXML private ImageView imgInfoAvatar;
    @FXML private Label lblInfoName, lblInfoRole;

    private boolean isInfoPanelOpen = false;

    private List<ConnectionModel> allFriendsList = new ArrayList<>();
    private WebSocketClientService webSocketService;
    private String currentChatPartner = null;
    private Popup attachmentMenuPopup;

    public static String targetUsernameToOpen = null;
    private Set<String> pinnedUsers = new HashSet<>();
    private Timeline onlineStatusTimeline;

    // =======================================================
    // BIẾN LƯU THỜI GIAN CỦA TIN NHẮN CUỐI CÙNG TRÊN MÀN HÌNH
    // =======================================================
    private LocalDateTime lastMessageTime = null;

    @FXML
    public void initialize() {
        vboxMessages.heightProperty().addListener((observable, oldValue, newValue) -> scrollMessages.setVvalue(1.0));
        loadFriendsList();
        GlobalWebSocketManager.setActiveChatListener(this::handleIncomingMessageUI);

        vboxMessages.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                GlobalWebSocketManager.currentActiveChatPartner = null;
                GlobalWebSocketManager.setActiveChatListener(null);
                if (onlineStatusTimeline != null) onlineStatusTimeline.stop();
            }
        });
    }

    @FXML
    public void goBack(javafx.scene.input.MouseEvent event) {
        try {
            GlobalWebSocketManager.currentActiveChatPartner = null;
            GlobalWebSocketManager.setActiveChatListener(null);
            if (onlineStatusTimeline != null) onlineStatusTimeline.stop();

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
        Platform.runLater(() -> processNewMessageState(msg));
    }

    private void processNewMessageState(ChatMessageModel msg) {
        String senderName = msg.sender.userName;

        boolean isRelevantChat =
                (senderName.equalsIgnoreCase(SessionManager.userName) && msg.receiver.userName.equalsIgnoreCase(currentChatPartner)) ||
                        (senderName.equalsIgnoreCase(currentChatPartner) && msg.receiver.userName.equalsIgnoreCase(SessionManager.userName));

        if (isRelevantChat) {
            // Lấy thời gian của tin nhắn mới
            LocalDateTime msgTime = parseTime(msg.sendTime);

            // Nếu cách tin trước đó quá 30 phút -> Chèn dải thời gian ở giữa
            if (lastMessageTime == null || ChronoUnit.MINUTES.between(lastMessageTime, msgTime) > 30) {
                vboxMessages.getChildren().add(createTimeSeparator(msgTime));
            }

            boolean isMe = senderName.equalsIgnoreCase(SessionManager.userName);
            vboxMessages.getChildren().add(createChatBubble(msg.content, isMe, msgTime));

            // Cập nhật lại mốc thời gian cuối
            lastMessageTime = msgTime;

        } else {
            if (!senderName.equalsIgnoreCase(SessionManager.userName)) {
                GlobalWebSocketManager.globalUnreadUsers.add(senderName.toLowerCase());
            }
        }

        // Kéo người dùng lên đầu danh sách bạn bè
        String targetUser = senderName.equalsIgnoreCase(SessionManager.userName) ? msg.receiver.userName : senderName;
        ConnectionModel targetConn = null;
        for (ConnectionModel conn : allFriendsList) {
            ConnectionModel.UserModel friend = conn.sender.userName.equalsIgnoreCase(SessionManager.userName) ? conn.receiver : conn.sender;
            if (friend.userName.equalsIgnoreCase(targetUser)) {
                targetConn = conn;
                break;
            }
        }

        if (targetConn != null) {
            allFriendsList.remove(targetConn);
            allFriendsList.add(0, targetConn);
            String searchKeyword = txtSearchFriend.getText();
            if (searchKeyword == null || searchKeyword.trim().isEmpty()) {
                renderFriendsList(allFriendsList);
            }
        }
    }

    // =======================================================
    // CÁC HÀM XỬ LÝ FORMAT THỜI GIAN
    // =======================================================
    private LocalDateTime parseTime(String timeString) {
        if (timeString == null || timeString.isEmpty()) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(timeString);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private String formatSeparatorTime(LocalDateTime time) {
        LocalDateTime now = LocalDateTime.now();
        if (time.toLocalDate().equals(now.toLocalDate())) {
            // Cùng ngày -> Chỉ hiện giờ (VD: 17:46)
            return time.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else {
            // Khác ngày -> Hiện Full (VD: 11 Thg 05 2026, 22:21)
            return time.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
        }
    }

    private HBox createTimeSeparator(LocalDateTime time) {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER);
        Label lblTime = new Label(formatSeparatorTime(time));
        lblTime.getStyleClass().add("chat-time-separator");
        box.getChildren().add(lblTime);
        VBox.setMargin(box, new Insets(15, 0, 5, 0));
        return box;
    }

    // =======================================================
    // TẠO BONG BÓNG CHAT + CHỨC NĂNG HOVER HIỆN GIỜ
    // =======================================================
    private HBox createChatBubble(String text, boolean isMe, LocalDateTime time) {
        HBox row = new HBox(8); // Khoảng cách giữa bong bóng và giờ

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(400);

        // Nút thời gian hiện ra khi Hover
        Label lblHoverTime = new Label(time.format(DateTimeFormatter.ofPattern("HH:mm")));
        lblHoverTime.getStyleClass().add("chat-hover-time");
        lblHoverTime.setVisible(false); // Mặc định ẩn

        if (isMe) {
            row.setAlignment(Pos.CENTER_RIGHT);
            bubble.getStyleClass().add("chat-bubble-me");
            // Mình nhắn thì Giờ nằm bên TRÁI bong bóng
            row.getChildren().addAll(lblHoverTime, bubble);
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            bubble.getStyleClass().add("chat-bubble-other");
            // Người khác nhắn thì Giờ nằm bên PHẢI bong bóng
            row.getChildren().addAll(bubble, lblHoverTime);
        }

        // Hiệu ứng Hover hiện giờ
        row.setOnMouseEntered(e -> lblHoverTime.setVisible(true));
        row.setOnMouseExited(e -> lblHoverTime.setVisible(false));

        return row;
    }

    private void loadChatHistory() {
        vboxMessages.getChildren().clear();
        lastMessageTime = null; // Reset lại biến đếm thời gian khi mở chat mới

        ApiService.getAsync("/chat/history?user1=" + SessionManager.userName + "&user2=" + currentChatPartner).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        java.lang.reflect.Type listType = new TypeToken<List<ChatMessageModel>>(){}.getType();
                        List<ChatMessageModel> history = ApiService.gson.fromJson(apiRes.result, listType);

                        for (ChatMessageModel msg : history) {
                            LocalDateTime msgTime = parseTime(msg.sendTime);
                            boolean isMe = msg.sender.userName.equalsIgnoreCase(SessionManager.userName);

                            // Nếu cách nhau > 30 phút thì chèn vạch thời gian
                            if (lastMessageTime == null || ChronoUnit.MINUTES.between(lastMessageTime, msgTime) > 30) {
                                vboxMessages.getChildren().add(createTimeSeparator(msgTime));
                            }

                            vboxMessages.getChildren().add(createChatBubble(msg.content, isMe, msgTime));
                            lastMessageTime = msgTime; // Lưu lại để tính cho vòng lặp tiếp theo
                        }

                        // Tự động cuộn xuống cuối (Đôi khi lịch sử quá dài, scrollbar chưa kịp ăn)
                        Platform.runLater(() -> scrollMessages.setVvalue(1.0));
                    }
                }
            });
        });
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

                        // TỰ ĐỘNG MỞ ĐOẠN CHAT NẾU ĐƯỢC CHUYỂN HƯỚNG TỪ TRANG CHỦ
                        if (targetUsernameToOpen != null) {
                            for (ConnectionModel conn : allFriendsList) {
                                ConnectionModel.UserModel friend = conn.sender.userName.equalsIgnoreCase(SessionManager.userName) ? conn.receiver : conn.sender;
                                if (friend.userName.equalsIgnoreCase(targetUsernameToOpen)) {

                                    // 1. Xóa chấm xanh của người này khỏi Global
                                    GlobalWebSocketManager.globalUnreadUsers.remove(friend.userName.toLowerCase());

                                    // 2. GỌI MAIN CONTROLLER ĐỂ GIẢM SỐ TRÊN SIDEBAR
                                    if (com.auction.controller.dashboard.MainController.getInstance() != null) {
                                        com.auction.controller.dashboard.MainController.getInstance().updateChatBadgeCount();
                                    }

                                    renderFriendsList(allFriendsList);
                                    openChatWith(friend);
                                    break;
                                }
                            }
                            targetUsernameToOpen = null;
                        }
                    }
                }
            });
        });
    }

    private void renderFriendsList(List<ConnectionModel> listToRender) {
        vboxFriends.getChildren().clear();
        if (listToRender == null || listToRender.isEmpty()) {
            Label lblEmpty = new Label("Không tìm thấy bạn bè nào.");
            lblEmpty.setStyle("-fx-padding: 10; -fx-text-fill: #7f8c8d;");
            vboxFriends.getChildren().add(lblEmpty);
            return;
        }

        List<ConnectionModel> sortedList = new ArrayList<>(listToRender);
        sortedList.sort((conn1, conn2) -> {
            ConnectionModel.UserModel f1 = conn1.sender.userName.equalsIgnoreCase(SessionManager.userName) ? conn1.receiver : conn1.sender;
            ConnectionModel.UserModel f2 = conn2.sender.userName.equalsIgnoreCase(SessionManager.userName) ? conn2.receiver : conn2.sender;

            boolean p1 = pinnedUsers.contains(f1.userName.toLowerCase());
            boolean p2 = pinnedUsers.contains(f2.userName.toLowerCase());
            if (p1 != p2) return p1 ? -1 : 1;

            boolean u1 = GlobalWebSocketManager.globalUnreadUsers.contains(f1.userName.toLowerCase());
            boolean u2 = GlobalWebSocketManager.globalUnreadUsers.contains(f2.userName.toLowerCase());
            if (u1 != u2) return u1 ? -1 : 1;

            return 0;
        });

        for (ConnectionModel conn : sortedList) {
            ConnectionModel.UserModel friend = conn.sender.userName.equalsIgnoreCase(SessionManager.userName) ? conn.receiver : conn.sender;
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
                    ConnectionModel.UserModel friend = conn.sender.userName.equalsIgnoreCase(SessionManager.userName) ? conn.receiver : conn.sender;
                    String friendName = friend.fullName != null ? friend.fullName : friend.userName;
                    return friendName.toLowerCase().contains(keyword);
                }).collect(Collectors.toList());
                renderFriendsList(filteredList);
            }
        });
    }

    private HBox createFriendItem(ConnectionModel.UserModel friend) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("friend-chat-item");

        if (currentChatPartner != null && currentChatPartner.equalsIgnoreCase(friend.userName)) {
            item.getStyleClass().add("active-chat");
        }

        ImageView avt = new ImageView(new Image(ApiService.BASE_URL + friend.avatarUrl, true));
        avt.setFitWidth(45); avt.setFitHeight(45);
        Rectangle clip = new Rectangle(45, 45); clip.setArcWidth(45); clip.setArcHeight(45);
        avt.setClip(clip);

        Label name = new Label(friend.fullName != null ? friend.fullName : friend.userName);
        name.getStyleClass().add("row-title-bold");

        boolean isPinned = pinnedUsers.contains(friend.userName.toLowerCase());
        SVGPath pinIcon = new SVGPath();
        pinIcon.setContent("M 16 12 V 4 h 1 V 2 H 7 v 2 h 1 v 8 l -2 2 v 2 h 5.2 v 6 h 1.6 v -6 H 18 v -2 l -2 -2 z");
        pinIcon.setFill(Color.web("#f39c12"));
        pinIcon.setScaleX(0.7); pinIcon.setScaleY(0.7);
        pinIcon.setVisible(isPinned);
        pinIcon.setManaged(isPinned);

        boolean isUnread = GlobalWebSocketManager.globalUnreadUsers.contains(friend.userName.toLowerCase());
        Circle unreadDot = new Circle(5, Color.web("#0084ff"));
        unreadDot.setVisible(isUnread);
        unreadDot.setManaged(isUnread);

        if (isUnread) {
            name.setStyle("-fx-font-weight: 900; -fx-font-size: 15px; -fx-text-fill: #0084ff;");
        } else {
            name.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");
        }

        HBox nameContainer = new HBox(6);
        nameContainer.setAlignment(Pos.CENTER_LEFT);
        nameContainer.getChildren().addAll(name, unreadDot);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnOptions = new Button();
        btnOptions.getStyleClass().add("btn-chat-options");
        SVGPath dotsIcon = new SVGPath();
        dotsIcon.setContent("M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z");
        dotsIcon.setFill(Color.web("#95a5a6"));
        dotsIcon.setScaleX(0.9); dotsIcon.setScaleY(0.9);
        btnOptions.setGraphic(dotsIcon);

        btnOptions.setVisible(false);
        btnOptions.setManaged(false);

        ContextMenu contextMenu = new ContextMenu();

        MenuItem pinItem = new MenuItem(isPinned ? "📌 Bỏ ghim tin nhắn" : "📌 Ghim tin nhắn");
        pinItem.setOnAction(e -> {
            if (isPinned) pinnedUsers.remove(friend.userName.toLowerCase());
            else pinnedUsers.add(friend.userName.toLowerCase());
            renderFriendsList(allFriendsList);
        });

        MenuItem muteItem = new MenuItem("🔕 Tắt thông báo");
        muteItem.setOnAction(e -> com.auction.util.ToastNotification.show("Tính năng", "Tính năng Tắt thông báo đang được cập nhật!"));

        MenuItem deleteChatItem = new MenuItem("🗑 Xóa tin nhắn");
        deleteChatItem.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có chắc chắn muốn xóa toàn bộ tin nhắn với " + friend.fullName + " không?", ButtonType.YES, ButtonType.NO);
            com.auction.util.AlertUtils.applyStyle(alert);
            alert.showAndWait().ifPresent(res -> {
                if (res == ButtonType.YES) {
                    if (currentChatPartner != null && currentChatPartner.equalsIgnoreCase(friend.userName)) {
                        vboxMessages.getChildren().clear();
                        lastMessageTime = null;
                    }
                    com.auction.util.ToastNotification.show("Thành công", "Đã xóa lịch sử tin nhắn!");
                }
            });
        });

        MenuItem unfriendItem = new MenuItem("❌ Hủy kết bạn");
        unfriendItem.getStyleClass().add("menu-item-delete");
        unfriendItem.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có chắc chắn muốn hủy kết bạn với " + friend.fullName + "?", ButtonType.YES, ButtonType.NO);
            com.auction.util.AlertUtils.applyStyle(alert);
            alert.showAndWait().ifPresent(res -> {
                if (res == ButtonType.YES) executeUnfriend(friend.userName);
            });
        });

        contextMenu.getItems().addAll(pinItem, muteItem, deleteChatItem, new SeparatorMenuItem(), unfriendItem);

        item.setOnMouseEntered(e -> {
            btnOptions.setVisible(true);
            btnOptions.setManaged(true);
        });

        item.setOnMouseExited(e -> {
            if (!contextMenu.isShowing()) {
                btnOptions.setVisible(false);
                btnOptions.setManaged(false);
            }
        });

        contextMenu.setOnHidden(e -> {
            if (!item.isHover()) {
                btnOptions.setVisible(false);
                btnOptions.setManaged(false);
            }
        });

        btnOptions.setOnAction(e -> {
            e.consume();
            contextMenu.getStyleClass().removeAll("dark-context-menu", "light-context-menu");
            if (SessionManager.isDarkMode) {
                contextMenu.getStyleClass().add("dark-context-menu");
            } else {
                contextMenu.getStyleClass().add("light-context-menu");
            }
            contextMenu.show(btnOptions, javafx.geometry.Side.BOTTOM, -130, 5);
        });

        btnOptions.setOnMouseClicked(javafx.scene.input.MouseEvent::consume);

        item.getChildren().addAll(avt, nameContainer, spacer, pinIcon, btnOptions);

        item.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {

                // Kiểm tra trực tiếp xem người này có chấm xanh trong Global không
                if (GlobalWebSocketManager.globalUnreadUsers.contains(friend.userName.toLowerCase())) {

                    // 1. Xóa chấm xanh của người này khỏi Global
                    GlobalWebSocketManager.globalUnreadUsers.remove(friend.userName.toLowerCase());

                    // 2. GỌI MAIN CONTROLLER ĐỂ GIẢM SỐ TRÊN SIDEBAR XUỐNG
                    if (com.auction.controller.dashboard.MainController.getInstance() != null) {
                        com.auction.controller.dashboard.MainController.getInstance().updateChatBadgeCount();
                    }

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
        Rectangle clip = new Rectangle(50, 50); clip.setArcWidth(50); clip.setArcHeight(50);
        imgChatAvatar.setClip(clip);

        paneWaiting.setVisible(false); paneWaiting.setManaged(false);
        chatHeader.setVisible(true); chatHeader.setManaged(true);
        scrollMessages.setVisible(true); scrollMessages.setManaged(true);
        paneInput.setVisible(true); paneInput.setManaged(true);

        loadChatHistory();
        startOnlineStatusChecker(friend.userName);

        if (isInfoPanelOpen) {
            updateInfoPanelData(friend);
        }
    }

    private void startOnlineStatusChecker(String username) {
        if (onlineStatusTimeline != null) {
            onlineStatusTimeline.stop();
        }

        setOnlineStatusUI(false);
        fetchOnlineStatus(username);

        onlineStatusTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            if (currentChatPartner != null && currentChatPartner.equalsIgnoreCase(username)) {
                fetchOnlineStatus(username);
            }
        }));

        onlineStatusTimeline.setCycleCount(Timeline.INDEFINITE);
        onlineStatusTimeline.play();
    }

    private void fetchOnlineStatus(String username) {
        ApiService.getAsync("/users/" + username + "/status").thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    boolean isOnline = res.body().contains("true") || res.body().toUpperCase().contains("ONLINE");
                    setOnlineStatusUI(isOnline);
                }
            });
        }).exceptionally(ex -> {
            return null;
        });
    }

    @FXML
    private void showChatInfo() {
        if (currentChatPartner == null) return;

        isInfoPanelOpen = !isInfoPanelOpen;
        paneFriendList.setVisible(!isInfoPanelOpen);
        paneFriendList.setManaged(!isInfoPanelOpen);
        paneChatInfo.setVisible(isInfoPanelOpen);
        paneChatInfo.setManaged(isInfoPanelOpen);

        if (isInfoPanelOpen) {
            ConnectionModel.UserModel partner = null;
            for (ConnectionModel conn : allFriendsList) {
                ConnectionModel.UserModel friend = conn.sender.userName.equalsIgnoreCase(SessionManager.userName) ? conn.receiver : conn.sender;
                if (friend.userName.equalsIgnoreCase(currentChatPartner)) {
                    partner = friend;
                    break;
                }
            }
            if (partner != null) updateInfoPanelData(partner);
        }
    }

    @FXML
    private void closeChatInfo() {
        isInfoPanelOpen = false;
        paneChatInfo.setVisible(false);
        paneChatInfo.setManaged(false);
        paneFriendList.setVisible(true);
        paneFriendList.setManaged(true);
    }

    @FXML
    private void handleViewProfile() {
        com.auction.util.ToastNotification.show("Tính năng", "Tính năng xem trang cá nhân đang được cập nhật!");
    }

    @FXML
    private void handleUnfriend() {
        if (currentChatPartner == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Hủy kết bạn với người này?", ButtonType.YES, ButtonType.NO);
        com.auction.util.AlertUtils.applyStyle(alert);
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) executeUnfriend(currentChatPartner);
        });
    }

    private void executeUnfriend(String targetUsername) {
        String url = "/chat/decline-request?sender=" + SessionManager.userName + "&receiver=" + targetUsername;
        ApiService.deleteAsync(url).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() >= 200 && res.statusCode() < 300) {
                    com.auction.util.ToastNotification.show("Thành công", "Đã xóa bạn bè!", com.auction.util.ToastNotification.ToastType.NOTIFICATION);
                    if (currentChatPartner != null && currentChatPartner.equalsIgnoreCase(targetUsername)) {
                        paneWaiting.setVisible(true); paneWaiting.setManaged(true);
                        chatHeader.setVisible(false); chatHeader.setManaged(false);
                        scrollMessages.setVisible(false); scrollMessages.setManaged(false);
                        paneInput.setVisible(false); paneInput.setManaged(false);
                        currentChatPartner = null;
                        closeChatInfo();
                    }
                    loadFriendsList();
                } else {
                    com.auction.util.ToastNotification.show("Lỗi", "Không thể xóa bạn bè lúc này!");
                }
            });
        });
    }

    private void updateInfoPanelData(ConnectionModel.UserModel partner) {
        lblInfoName.setText(partner.fullName != null ? partner.fullName : partner.userName);
        imgInfoAvatar.setImage(new Image(ApiService.BASE_URL + partner.avatarUrl, true));

        Rectangle clip = new Rectangle(100, 100);
        clip.setArcWidth(100); clip.setArcHeight(100);
        imgInfoAvatar.setClip(clip);

        lblInfoRole.setText("Đang tải...");

        ApiService.getAsync("/users/profile/" + partner.userName).thenAccept(res -> {
            Platform.runLater(() -> {
                String roleDisplay = "Thành viên (Bidder)";
                if (res.statusCode() >= 200 && res.statusCode() < 300) {
                    try {
                        ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        if (apiRes.code == 1000) {
                            com.auction.model.UserModel fullUser = ApiService.gson.fromJson(apiRes.result, com.auction.model.UserModel.class);
                            if ("SELLER".equals(fullUser.role)) {
                                roleDisplay = "Người bán (Seller)";
                            } else if ("ADMIN".equals(fullUser.role)) {
                                roleDisplay = "Quản trị viên (Admin)";
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Lỗi đọc JSON Role: " + e.getMessage());
                    }
                }
                lblInfoRole.setText(roleDisplay);
            });
        });
    }

    @FXML
    private void sendMessage() {
        String text = txtMessage.getText().trim();
        if (text.isEmpty() || currentChatPartner == null) return;
        GlobalWebSocketManager.sendMessage(SessionManager.userName, currentChatPartner, text);

        ConnectionModel targetConn = null;
        for (ConnectionModel conn : allFriendsList) {
            ConnectionModel.UserModel friend = conn.sender.userName.equalsIgnoreCase(SessionManager.userName) ? conn.receiver : conn.sender;
            if (friend.userName.equalsIgnoreCase(currentChatPartner)) {
                targetConn = conn;
                break;
            }
        }
        if (targetConn != null) {
            allFriendsList.remove(targetConn);
            allFriendsList.add(0, targetConn);

            String searchKeyword = txtSearchFriend.getText();
            if (searchKeyword == null || searchKeyword.trim().isEmpty()) {
                renderFriendsList(allFriendsList);
            }
        }

        txtMessage.clear();
    }

    @FXML
    private void showAttachmentMenu() {
        if (attachmentMenuPopup == null) {
            attachmentMenuPopup = new Popup();
            attachmentMenuPopup.setAutoHide(true);

            VBox attachmentMenuBox = new VBox();
            attachmentMenuBox.getStyleClass().add("card");
            attachmentMenuBox.setStyle("-fx-padding: 5; -fx-background-radius: 8; -fx-border-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 3);");

            if (btnAttachment.getScene() != null) {
                attachmentMenuBox.getStylesheets().addAll(btnAttachment.getScene().getStylesheets());
            }

            String imgSvg = "M 21 19 V 5 c 0 -1.1 -.9 -2 -2 -2 H 5 c -1.1 0 -2 .9 -2 2 v 14 c 0 1.1 .9 2 2 2 h 14 c 1.1 0 2 -.9 2 -2 z M 8.5 13.5 l 2.5 3.01 L 14.5 12 l 4.5 6 H 5 l 3.5 -4.5 z";
            String clipSvg = "M 16.5 6 v 11.5 c 0 2.21 -1.79 4 -4 4 s -4 -1.79 -4 -4 V 5 a 2.5 2.5 0 0 1 5 0 v 10.5 c 0 .55 -.45 1 -1 1 s -1 -.45 -1 -1 V 6 H 10 v 9.5 a 2.5 2.5 0 0 0 5 0 V 5 c 0 -2.21 -1.79 -4 -4 -4 S 7 2.79 7 5 v 12.5 c 0 3.04 2.46 5.5 5.5 -5.5 s 5.5 -2.46 5.5 -5.5 V 6 h -1.5 z";

            HBox btnImage = createPopupMenuItem("Thêm ảnh", imgSvg, () -> attachmentMenuPopup.hide());
            HBox btnFile = createPopupMenuItem("Thêm đính kèm", clipSvg, () -> attachmentMenuPopup.hide());

            attachmentMenuBox.getChildren().addAll(btnImage, btnFile);
            attachmentMenuPopup.getContent().add(attachmentMenuBox);
        }

        VBox box = (VBox) attachmentMenuPopup.getContent().get(0);
        box.getStyleClass().remove("dark-theme");
        if (SessionManager.isDarkMode) {
            box.getStyleClass().add("dark-theme");
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
            if (onlineStatusTimeline != null) onlineStatusTimeline.stop();

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/auction/view/chat/FriendList.fxml"));
            javafx.scene.Node view = loader.load();
            javafx.scene.Node source = (javafx.scene.Node) event.getSource();
            javafx.scene.layout.Pane contentArea = (javafx.scene.layout.Pane) source.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setOnlineStatusUI(boolean isOnline) {
        if (isOnline) {
            iconOnlineStatus.setFill(Color.web("#2ecc71"));
            lblOnlineStatus.setText("Đang hoạt động");
        } else {
            iconOnlineStatus.setFill(Color.web("#95a5a6"));
            lblOnlineStatus.setText("Ngoại tuyến");
        }
    }
}
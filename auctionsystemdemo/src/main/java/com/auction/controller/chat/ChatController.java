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
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import java.util.ArrayList;
import java.util.stream.Collectors;
import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Popup;

import java.util.List;

public class ChatController {

    @FXML private VBox vboxFriends, vboxMessages, paneWaiting;
    @FXML private HBox chatHeader, paneInput;
    @FXML private Label lblChatName;
    @FXML private ImageView imgChatAvatar;
    @FXML private TextField txtMessage;
    @FXML private ScrollPane scrollMessages;
    @FXML private TextField txtSearchFriend;
    @FXML private javafx.scene.control.Button btnAttachment;

    // Biến lưu trữ danh sách bạn bè gốc để khi xoá ô tìm kiếm nó hiện lại đầy đủ
    private List<ConnectionModel> allFriendsList = new ArrayList<>();

    private WebSocketClientService webSocketService;
    private String currentChatPartner = null; // Người đang chat hiện tại


    // Biến lưu trữ Popup để không phải tạo lại nhiều lần
    private Popup attachmentMenuPopup;

    @FXML
    public void initialize() {
        // Tự động cuộn xuống cuối khi có tin nhắn mới
        vboxMessages.heightProperty().addListener((observable, oldValue, newValue) -> scrollMessages.setVvalue(1.0));

        loadFriendsList();

        // Nhận tin nhắn từ Global
        GlobalWebSocketManager.setActiveChatListener(this::handleIncomingMessageUI);

        // ==========================================================
        // ĐÃ SỬA LẠI THÀNH SCENE PROPERTY (ĐẢM BẢO CHUẨN XÁC 100%)
        // ==========================================================
        vboxMessages.sceneProperty().addListener((observable, oldScene, newScene) -> {
            // Khi newScene == null nghĩa là Giao diện Chat đã thực sự bị xóa khỏi cửa sổ hiển thị
            if (newScene == null) {
                System.out.println("🔥 HỆ THỐNG: Giao diện Chat đã đóng -> Bật lại thông báo Toast!");
                GlobalWebSocketManager.currentActiveChatPartner = null;
                GlobalWebSocketManager.setActiveChatListener(null);
            }
        });
        // ==========================================================
    }

    // ========================================================
    // XỬ LÝ NÚT QUAY LẠI (BACK) ĐỒNG BỘ VỚI WALLET
    // ========================================================
    @FXML
    public void goBack(javafx.scene.input.MouseEvent event) {
        try {
            // Reset lại partner để đi ra chỗ khác sẽ nhận được Toast
            GlobalWebSocketManager.currentActiveChatPartner = null;
            GlobalWebSocketManager.setActiveChatListener(null);
            // Đổi đường dẫn "/com/auction/view/home/Home.fxml" thành FXML bạn muốn quay lại
            // (Ví dụ trang chủ hoặc dashboard)
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/auction/view/home/Home.fxml"));
            javafx.scene.Node view = loader.load();
            javafx.scene.Node source = (javafx.scene.Node) event.getSource();
            javafx.scene.layout.Pane contentArea = (javafx.scene.layout.Pane) source.getScene().lookup("#contentArea");

            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupWebSocket() {
        webSocketService = new WebSocketClientService();
        webSocketService.connect(SessionManager.userName, this::handleIncomingMessage);
    }

    // THÊM HÀM NÀY ĐỂ XỬ LÝ UI KHI CÓ TIN NHẮN TỪ GLOBAL ĐẨY VÀO
    private void handleIncomingMessageUI(ChatMessageModel msg) {
        if (msg == null) return;

        // Chỉ render khung chat nếu tin nhắn này thuộc về cuộc hội thoại đang mở
        boolean isRelevantChat =
                (msg.sender.userName.equals(SessionManager.userName) && msg.receiver.userName.equals(currentChatPartner)) ||
                        (msg.sender.userName.equals(currentChatPartner) && msg.receiver.userName.equals(SessionManager.userName));

        if (isRelevantChat) {
            boolean isMe = msg.sender.userName.equals(SessionManager.userName);
            vboxMessages.getChildren().add(createChatBubble(msg.content, isMe));
        }
    }

    // ========================================================
    // HỨNG TIN NHẮN TỪ SERVER BẮN VỀ (BỔ SUNG BẮT LỖI HIỂN THỊ)
    // ========================================================
    private void handleIncomingMessage(String jsonPayload) {
        // HÃY NHÌN VÀO CONSOLE ĐỂ XEM DÒNG NÀY IN RA CÁI GÌ! NẾU LÀ [B@... THÌ LÀ DO BACKEND
        System.out.println("📩 RAW JSON NHẬN ĐƯỢC: " + jsonPayload);

        Platform.runLater(() -> {
            try {
                String cleanJson = jsonPayload.trim();
                ChatMessageModel msg = null;

                // KIỂM TRA: Nếu chuỗi bắt đầu bằng "[" (Nghĩa là Mảng)
                if (cleanJson.startsWith("[")) {
                    java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<ChatMessageModel>>(){}.getType();
                    java.util.List<ChatMessageModel> list = ApiService.gson.fromJson(cleanJson, listType);
                    if (list != null && !list.isEmpty()) {
                        msg = list.get(0); // Rút tin nhắn đầu tiên ra
                    }
                } else {
                    // Bình thường: Dịch dưới dạng Object
                    msg = ApiService.gson.fromJson(cleanJson, ChatMessageModel.class);
                }

                // Nếu lấy được tin nhắn thành công thì in ra màn hình
                if (msg != null) {
                    boolean isRelevantChat =
                            (msg.sender.userName.equals(SessionManager.userName) && msg.receiver.userName.equals(currentChatPartner)) ||
                                    (msg.sender.userName.equals(currentChatPartner) && msg.receiver.userName.equals(SessionManager.userName));

                    if (isRelevantChat) {
                        boolean isMe = msg.sender.userName.equals(SessionManager.userName);
                        vboxMessages.getChildren().add(createChatBubble(msg.content, isMe));
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("❌ KHÔNG THỂ DỊCH JSON: " + jsonPayload);
            }
        });
    }

    private void loadFriendsList() {
        ApiService.getAsync("/chat/friends?username=" + SessionManager.userName).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        java.lang.reflect.Type listType = new TypeToken<List<ConnectionModel>>(){}.getType();
                        // Lưu vào biến toàn cục để dành cho việc lọc
                        allFriendsList = ApiService.gson.fromJson(apiRes.result, listType);

                        // Hiển thị lần đầu tiên
                        renderFriendsList(allFriendsList);

                        // Kích hoạt tính năng tìm kiếm
                        setupSearchFeature();
                    }
                }
            });
        });
    }

    // Hàm in danh sách ra màn hình (Tách riêng ra để dùng lại khi tìm kiếm)
    private void renderFriendsList(List<ConnectionModel> listToRender) {
        vboxFriends.getChildren().clear();
        if (listToRender == null || listToRender.isEmpty()) {
            Label lblEmpty = new Label("Không tìm thấy bạn bè nào.");
            lblEmpty.setStyle("-fx-padding: 10; -fx-text-fill: #7f8c8d;");
            vboxFriends.getChildren().add(lblEmpty);
        } else {
            for (ConnectionModel conn : listToRender) {
                ConnectionModel.UserModel friend = conn.sender.userName.equals(SessionManager.userName) ? conn.receiver : conn.sender;
                vboxFriends.getChildren().add(createFriendItem(friend));
            }
        }
    }

    // Bắt sự kiện mỗi khi người dùng gõ phím vào ô tìm kiếm
    private void setupSearchFeature() {
        txtSearchFriend.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                // Nếu xoá hết chữ -> Hiển thị lại toàn bộ
                renderFriendsList(allFriendsList);
            } else {
                // Lọc danh sách không phân biệt hoa thường
                String keyword = newValue.toLowerCase().trim();
                List<ConnectionModel> filteredList = allFriendsList.stream().filter(conn -> {
                    ConnectionModel.UserModel friend = conn.sender.userName.equals(SessionManager.userName) ? conn.receiver : conn.sender;
                    String friendName = friend.fullName != null ? friend.fullName : friend.userName;
                    return friendName.toLowerCase().contains(keyword);
                }).collect(Collectors.toList());

                // Hiển thị kết quả lọc
                renderFriendsList(filteredList);
            }
        });
    }

    private HBox createFriendItem(ConnectionModel.UserModel friend) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setStyle("-fx-padding: 10 15; -fx-background-radius: 10; -fx-cursor: hand;");

        ImageView avt = new ImageView(new Image(ApiService.BASE_URL + friend.avatarUrl, true));
        avt.setFitWidth(45); avt.setFitHeight(45);
        Rectangle clip = new Rectangle(45, 45); clip.setArcWidth(45); clip.setArcHeight(45);
        avt.setClip(clip);

        Label name = new Label(friend.fullName != null ? friend.fullName : friend.userName);
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #2c3e50;");

        item.getChildren().addAll(avt, name);

        // Hiệu ứng Hover
        item.setOnMouseEntered(e -> item.setStyle("-fx-padding: 10 15; -fx-background-radius: 10; -fx-cursor: hand; -fx-background-color: #f1f2f6;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-padding: 10 15; -fx-background-radius: 10; -fx-cursor: hand; -fx-background-color: transparent;"));

        // Khi click vào bạn bè -> Mở chat
        item.setOnMouseClicked(e -> openChatWith(friend));

        return item;
    }

    private void openChatWith(ConnectionModel.UserModel friend) {
        currentChatPartner = friend.userName;

        // BÁO CHO GLOBAL MANAGER BIẾT ĐỂ NÓ KHÔNG HIỆN TOAST THỪA
        GlobalWebSocketManager.currentActiveChatPartner = currentChatPartner;

        lblChatName.setText(friend.fullName != null ? friend.fullName : friend.userName);
        imgChatAvatar.setImage(new Image(ApiService.BASE_URL + friend.avatarUrl, true));
        Rectangle clip = new Rectangle(45, 45); clip.setArcWidth(45); clip.setArcHeight(45);
        imgChatAvatar.setClip(clip);

        // Bật giao diện chat
        paneWaiting.setVisible(false); paneWaiting.setManaged(false);
        chatHeader.setVisible(true); chatHeader.setManaged(true);
        scrollMessages.setVisible(true); scrollMessages.setManaged(true);
        paneInput.setVisible(true); paneInput.setManaged(true);

        loadChatHistory();
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
        bubble.setMaxWidth(400); // Giới hạn chiều rộng tin nhắn

        if (isMe) {
            row.setAlignment(Pos.CENTER_RIGHT);
            bubble.setStyle("-fx-background-color: #0A439D; -fx-text-fill: white; -fx-padding: 10 15; -fx-background-radius: 15 15 0 15; -fx-font-size: 14.5px;");
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            bubble.setStyle("-fx-background-color: #f1f2f6; -fx-text-fill: #2c3e50; -fx-padding: 10 15; -fx-background-radius: 15 15 15 0; -fx-font-size: 14.5px;");
        }

        row.getChildren().add(bubble);
        return row;
    }

    // SỬA HÀM SEND MESSAGE ĐỂ GỌI GLOBAL
    @FXML
    private void sendMessage() {
        String text = txtMessage.getText().trim();
        if (text.isEmpty() || currentChatPartner == null) return;

        // GỌI GLOBAL MANAGER
        GlobalWebSocketManager.sendMessage(SessionManager.userName, currentChatPartner, text);
        txtMessage.clear();
    }

    // ========================================================
    // XỬ LÝ NÚT THÊM ĐÍNH KÈM (+) - ĐỒNG BỘ VỚI STYLE.CSS
    // ========================================================
    @FXML
    private void showAttachmentMenu() {
        if (attachmentMenuPopup == null) {
            attachmentMenuPopup = new Popup();
            attachmentMenuPopup.setAutoHide(true); // Tự động đóng khi click ra ngoài

            // Khung bên ngoài: Đồng bộ chuẩn với .combo-box-popup .list-view trong style.css
            VBox menuBox = new VBox();
            menuBox.setStyle("-fx-background-color: white; " +
                    "-fx-background-radius: 8; -fx-border-radius: 8; " +
                    "-fx-border-color: #ecf0f1; " +
                    "-fx-padding: 5; " + // Padding 5 tạo khoảng trống giữa mép và các ô bấm
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 3);");

            // Vector Icon: Bức ảnh
            String imgSvg = "M 21 19 V 5 c 0 -1.1 -.9 -2 -2 -2 H 5 c -1.1 0 -2 .9 -2 2 v 14 c 0 1.1 .9 2 2 2 h 14 c 1.1 0 2 -.9 2 -2 z M 8.5 13.5 l 2.5 3.01 L 14.5 12 l 4.5 6 H 5 l 3.5 -4.5 z";
            // Vector Icon: Ghim giấy
            String clipSvg = "M 16.5 6 v 11.5 c 0 2.21 -1.79 4 -4 4 s -4 -1.79 -4 -4 V 5 a 2.5 2.5 0 0 1 5 0 v 10.5 c 0 .55 -.45 1 -1 1 s -1 -.45 -1 -1 V 6 H 10 v 9.5 a 2.5 2.5 0 0 0 5 0 V 5 c 0 -2.21 -1.79 -4 -4 -4 S 7 2.79 7 5 v 12.5 c 0 3.04 2.46 5.5 5.5 -5.5 s 5.5 -2.46 5.5 -5.5 V 6 h -1.5 z";

            HBox btnImage = createPopupMenuItem("Thêm ảnh", imgSvg, () -> {
                System.out.println("Bạn vừa bấm: Thêm ảnh");
                // TODO: Code mở chọn ảnh ở đây
                attachmentMenuPopup.hide();
            });

            HBox btnFile = createPopupMenuItem("Thêm đính kèm", clipSvg, () -> {
                System.out.println("Bạn vừa bấm: Thêm đính kèm");
                // TODO: Code mở chọn file ở đây
                attachmentMenuPopup.hide();
            });

            menuBox.getChildren().addAll(btnImage, btnFile);
            attachmentMenuPopup.getContent().add(menuBox);
        }

        // Lấy toạ độ để hiển thị popup
        javafx.geometry.Bounds bounds = btnAttachment.localToScreen(btnAttachment.getBoundsInLocal());
        attachmentMenuPopup.show(btnAttachment, bounds.getMinX() - 10, bounds.getMinY() - 105);
    }

    // Hàm hỗ trợ vẽ từng dòng trong Menu (Vẽ Icon Vector + Chữ + Hiệu ứng Hover)
    private HBox createPopupMenuItem(String text, String svgPath, Runnable action) {
        // Icon Vector
        SVGPath icon = new SVGPath();
        icon.setContent(svgPath);
        icon.setFill(Color.web("#333333")); // Màu xám đen mặc định
        icon.setScaleX(0.8);
        icon.setScaleY(0.8);

        // Chữ
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #333333; -fx-font-size: 14px;");

        // Gộp Icon và Text
        HBox box = new HBox(15, icon, label);
        box.setAlignment(Pos.CENTER_LEFT);

        // Mặc định: Bo vuông góc (radius = 5) theo CSS của bạn
        box.setStyle("-fx-padding: 8 15; -fx-background-color: transparent; -fx-background-radius: 5; -fx-cursor: hand; -fx-pref-width: 200;");

        // HIỆU ỨNG KHI DI CHUỘT VÀO: Đổi sang màu Xanh đậm (#3b5998) giống y hệt style.css
        box.setOnMouseEntered(e -> {
            box.setStyle("-fx-padding: 8 15; -fx-background-color: #3b5998; -fx-background-radius: 5; -fx-cursor: hand; -fx-pref-width: 200;");
            label.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
            icon.setFill(Color.WHITE);
        });

        // HIỆU ỨNG KHI CHUỘT RỜI ĐI
        box.setOnMouseExited(e -> {
            box.setStyle("-fx-padding: 8 15; -fx-background-color: transparent; -fx-background-radius: 5; -fx-cursor: hand; -fx-pref-width: 200;");
            label.setStyle("-fx-text-fill: #333333; -fx-font-size: 14px;");
            icon.setFill(Color.web("#333333"));
        });

        // Xử lý sự kiện bấm
        box.setOnMouseClicked(e -> action.run());

        return box;
    }

    // ========================================================
    // XỬ LÝ NÚT "DANH SÁCH KẾT BẠN" -> CHUYỂN SANG TRANG FRIEND LIST
    // ========================================================
    @FXML
    private void showAllFriends(javafx.event.ActionEvent event) {
        try {
            // BỔ SUNG LỆNH RESET VÀO ĐÂY GIỐNG NHƯ HÀM goBack()
            GlobalWebSocketManager.currentActiveChatPartner = null;
            GlobalWebSocketManager.setActiveChatListener(null);

            // Chuyển sang giao diện FriendList
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/auction/view/chat/FriendList.fxml"));
            javafx.scene.Node view = loader.load();
            javafx.scene.Node source = (javafx.scene.Node) event.getSource();
            javafx.scene.layout.Pane contentArea = (javafx.scene.layout.Pane) source.getScene().lookup("#contentArea");

            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
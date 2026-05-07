package com.auction.controller;

import com.auction.model.*; // Import tất cả model cần thiết
import com.auction.util.ApiService;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets; // Cần import Insets
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Callback; // Cần import Callback

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

public class AdminController {

    // TAB ITEM
    @FXML private TableView<ItemModel> tbItems;
    @FXML private TableColumn<ItemModel, String> colItemId, colItemName, colItemType, colItemPrice, colItemSeller;

    // TAB AUCTION
    @FXML private TableView<AuctionModel> tbAuctions;
    @FXML private TableColumn<AuctionModel, String> colAucId, colAucProduct, colAucHighestBid, colAucStatus, colAucWinner;
    @FXML private TableColumn<AuctionModel, Void> colAucActions;

    // TAB USER
    @FXML private TableView<UserModel> tbUsers;
    @FXML private TableColumn<UserModel, String> colUserCode, colUsername, colUserFullName, colUserRole, colUserStatus;
    @FXML private TableColumn<UserModel, Void> colUserActions;

    @FXML
    public void initialize() {
        setupItemTable();
        setupAuctionTable();
        setupUserTable();
        loadAllData();
    }

    @FXML
    public void loadAllData() {
        loadItems();
        loadAuctions();
        loadUsers();
    }

    // ==========================================
    // 1. SETUP TAB ITEMS
    // ==========================================
    private void setupItemTable() {
        colItemId.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().id));
        colItemName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().name));
        colItemType.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().itemType != null ? cell.getValue().itemType : "N/A"));
        colItemPrice.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%,.0f", cell.getValue().startPrice)));
        colItemSeller.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().seller != null ? cell.getValue().seller.userName : "Admin"));
    }

    private void loadItems() {
        ApiService.getAsync("/items").thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) { // Chỉ xử lý khi API trả về thành công
                        Type listType = new TypeToken<List<ItemModel>>(){}.getType();
                        List<ItemModel> list = ApiService.gson.fromJson(apiRes.result, listType);
                        tbItems.setItems(FXCollections.observableArrayList(list));
                    } else {
                        // Xử lý lỗi API nếu cần
                        System.out.println("Lỗi khi tải Items: " + apiRes.message);
                    }
                } else {
                    System.out.println("Lỗi mạng hoặc server khi tải Items: " + res.statusCode());
                }
            });
        });
    }

    // ==========================================
    // 2. SETUP TAB AUCTIONS
    // ==========================================
    private void setupAuctionTable() {
        colAucId.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().id.substring(0, 8) + "..."));
        colAucProduct.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().bidProduct != null ? cell.getValue().bidProduct.name : "Sản phẩm ẩn"));
        colAucHighestBid.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%,.0f", cell.getValue().highestBid)));
        colAucStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().status));
        colAucWinner.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().winningUser != null ? cell.getValue().winningUser.userName : "None"));

        // Thêm nút Xem lịch sử Bid (đã sửa style thành viên thuốc)
        colAucActions.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Lịch sử Bid");
            {
                btn.setStyle("-fx-background-color: #3b5998; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 20; -fx-padding: 5 15; -fx-font-weight: bold;");
                btn.setOnAction(e -> {
                    AuctionModel auction = getTableView().getItems().get(getIndex());
                    showBidHistory(auction);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    // Bọc nút vào HBox để căn giữa theo chiều dọc
                    HBox pane = new HBox(btn);
                    pane.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    setGraphic(pane);
                }
            }
        });
    }

    private void showBidHistory(AuctionModel auction) {
        StringBuilder sb = new StringBuilder("LỊCH SỬ ĐẶT GIÁ SẢN PHẨM: " + auction.bidProduct.name + "\n\n");
        if (auction.bidTransactions == null || auction.bidTransactions.isEmpty()) {
            sb.append("Chưa có lượt đặt giá nào.");
        } else {
            for (AuctionModel.BidTransactionModel tx : auction.bidTransactions) {
                sb.append("- ").append(tx.bidder != null ? tx.bidder.userName : "Unknown Bidder")
                        .append(" : ").append(String.format("%,.0f", tx.bidAmount)).append("\n");
            }
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Lịch sử Bid");
        alert.setHeaderText(null);
        alert.setContentText(sb.toString());
        alert.show();
    }

    private void loadAuctions() {
        ApiService.getAsync("/auctions").thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        Type listType = new TypeToken<List<AuctionModel>>(){}.getType();
                        List<AuctionModel> list = ApiService.gson.fromJson(apiRes.result, listType);
                        tbAuctions.setItems(FXCollections.observableArrayList(list));
                    } else {
                        System.out.println("Lỗi khi tải Auctions: " + apiRes.message);
                    }
                } else {
                    System.out.println("Lỗi mạng hoặc server khi tải Auctions: " + res.statusCode());
                }
            });
        });
    }

    // ==========================================
    // 3. SETUP TAB USERS
    // ==========================================
    private void setupUserTable() {
        colUserCode.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().userCode));
        colUsername.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().userName));
        colUserFullName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().fullName));
        colUserRole.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().role));
        colUserStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().banned ? "Bị khóa" : "Hoạt động"));

        colUserActions.setCellFactory(param -> new TableCell<>() {
            // Đổi text cho giống 100% trong ảnh
            private final Button btnBan = new Button("Khóa / Mở");
            private final Button btnDelete = new Button("Xóa");
            private final Button btnUpgrade = new Button("Up seller");

            // Khoảng cách giữa các nút là 10
            private final HBox pane = new HBox(10, btnBan, btnDelete, btnUpgrade);

            {
                pane.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                // Style chung: Bo tròn viên thuốc, KHÔNG in đậm để giống ảnh, kích thước chữ vừa phải
                String commonStyle = "-fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 20; -fx-padding: 6 15; -fx-font-size: 13px;";

                // Set màu giống ảnh
                btnBan.setStyle("-fx-background-color: #f39c12; " + commonStyle);
                btnDelete.setStyle("-fx-background-color: #e74c3c; " + commonStyle);
                btnUpgrade.setStyle("-fx-background-color: #58d68d; " + commonStyle); // Xanh lá dịu

                btnBan.setOnAction(e -> toggleBanUser(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> deleteUser(getTableView().getItems().get(getIndex())));
                btnUpgrade.setOnAction(e -> upgradeUser(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    UserModel user = getTableView().getItems().get(getIndex());
                    btnUpgrade.setVisible(!"ADMIN".equals(user.role) && !"SELLER".equals(user.role));
                    btnBan.setVisible(!"ADMIN".equals(user.role));
                    btnDelete.setVisible(!"ADMIN".equals(user.role));
                    setGraphic(pane);
                }
            }
        });
    }

    private void loadUsers() {
        ApiService.getAsync("/users/admin").thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        Type listType = new TypeToken<List<UserModel>>(){}.getType();
                        List<UserModel> list = ApiService.gson.fromJson(apiRes.result, listType);
                        tbUsers.setItems(FXCollections.observableArrayList(list));
                    } else {
                        System.out.println("Lỗi khi tải Users: " + apiRes.message);
                    }
                } else {
                    System.out.println("Lỗi mạng hoặc server khi tải Users: " + res.statusCode());
                }
            });
        });
    }

    private void toggleBanUser(UserModel user) {
        // Gọi API sử dụng user.userName
        ApiService.putAsync("/users/admin/" + user.userName + "/ban", null).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() >= 200 && res.statusCode() < 300) {
                    loadUsers(); // Tải lại danh sách để cập nhật trạng thái
                } else {
                    System.out.println("Lỗi khi ban/unban user: " + res.body());
                }
            });
        });
    }

    private void deleteUser(UserModel user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Bạn chắc chắn muốn xóa người dùng " + user.userName + "?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                // Gọi API sử dụng user.userName
                ApiService.deleteAsync("/users/admin/" + user.userName).thenAccept(res -> {
                    Platform.runLater(() -> {
                        if (res.statusCode() >= 200 && res.statusCode() < 300) {
                            loadUsers(); // Tải lại danh sách sau khi xóa
                        } else {
                            System.out.println("Lỗi khi xóa user: " + res.body());
                        }
                    });
                });
            }
        });
    }

    private void upgradeUser(UserModel user) {
        ApiService.putAsync("/users/upgrade-to-seller/" + user.userName, null).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() >= 200 && res.statusCode() < 300) {
                    new Alert(Alert.AlertType.INFORMATION, "Đã cấp quyền Seller cho " + user.userName + " thành công!").show();
                    loadUsers(); // Tải lại danh sách để cập nhật vai trò
                } else {
                    // Thử parse lỗi từ backend
                    try {
                        ApiResponse errRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        new Alert(Alert.AlertType.ERROR, "Lỗi cấp quyền: " + errRes.message).show();
                    } catch (Exception e) {
                        new Alert(Alert.AlertType.ERROR, "Lỗi cấp quyền: Mã lỗi " + res.statusCode()).show();
                    }
                }
            });
        });
    }

    @FXML
    private void goBack() {
        try {
            Node view = FXMLLoader.load(getClass().getResource("/com/auction/view/Profile.fxml"));
            StackPane contentArea = (StackPane) tbItems.getScene().lookup("#contentArea");
            if(contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
package com.auction.controller.profile;

import com.auction.model.*;
import com.auction.util.ApiService;
import com.auction.util.AlertUtils; // IMPORT ALERTUTILS
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.lang.reflect.Type;
import java.util.List;

public class AdminController {

    @FXML private TableColumn<ItemModel, Void> colItemActions;

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
        colItemActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnCancel = new Button("Hủy SP");
            {
                btnCancel.getStyleClass().addAll("btn-table", "btn-delete");
                btnCancel.setOnAction(e -> {
                    ItemModel item = getTableView().getItems().get(getIndex());
                    cancelItem(item);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(btnCancel);
            }
        });
    }

    private void cancelItem(ItemModel item) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Hủy sản phẩm");
        dialog.setHeaderText("Hủy sản phẩm: " + item.name);
        dialog.setContentText("Nhập lý do hủy:");

        // THÊM CSS CHO TEXT INPUT DIALOG
        AlertUtils.applyStyle(dialog);

        dialog.showAndWait().ifPresent(reason -> {
            if (reason.trim().isEmpty()) {
                Alert warnAlert = new Alert(Alert.AlertType.WARNING, "Vui lòng nhập lý do!");
                AlertUtils.applyStyle(warnAlert); // THÊM CSS
                warnAlert.show();
                return;
            }

            java.util.Map<String, String> requestBody = new java.util.HashMap<>();
            requestBody.put("reason", reason);

            ApiService.putAsync("/items/cancel/" + item.id, requestBody).thenAccept(res -> {
                Platform.runLater(() -> {
                    if (res.statusCode() >= 200 && res.statusCode() < 300) {
                        Alert successAlert = new Alert(Alert.AlertType.INFORMATION, "Đã hủy sản phẩm thành công!");
                        AlertUtils.applyStyle(successAlert); // THÊM CSS
                        successAlert.show();
                        loadItems();
                    } else {
                        String errorMsg = res.body();
                        try {
                            ApiResponse errRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                            if (errRes != null && errRes.message != null) {
                                errorMsg = errRes.message;
                            }
                        } catch (Exception ignored) {}

                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle("Lỗi hủy sản phẩm");
                        errorAlert.setHeaderText("Hệ thống từ chối yêu cầu!");

                        TextArea area = new TextArea("Chi tiết lỗi:\n" + errorMsg);
                        area.setWrapText(true);
                        area.setEditable(false);
                        area.setPrefSize(500, 200);

                        errorAlert.getDialogPane().setContent(area);
                        AlertUtils.applyStyle(errorAlert); // THÊM CSS
                        errorAlert.show();
                    }
                });
            });
        });
    }

    private void loadItems() {
        ApiService.getAsync("/items").thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        Type listType = new TypeToken<List<ItemModel>>(){}.getType();
                        List<ItemModel> list = ApiService.gson.fromJson(apiRes.result, listType);
                        tbItems.setItems(FXCollections.observableArrayList(list));
                    }
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

        colAucActions.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Lịch sử Bid");
            // SỬA LỖI 1: KHỞI TẠO HBOX Ở ĐÂY ĐỂ TRÁNH MẤT NÚT KHI CUỘN BẢNG
            private final HBox pane = new HBox(btn);

            {
                btn.getStyleClass().addAll("btn-table", "btn-history");
                pane.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

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
                    setGraphic(pane); // Chỉ set Graphic, không new HBox nữa
                }
            }
        });
    }

    private void showBidHistory(AuctionModel auction) {
        // SỬA LỖI 2: CHỐNG NULL POINTER KHIẾN POPUP KHÔNG THỂ HIỆN LÊN
        String productName = (auction.bidProduct != null && auction.bidProduct.name != null)
                ? auction.bidProduct.name : "Sản phẩm không xác định";

        StringBuilder sb = new StringBuilder("LỊCH SỬ ĐẶT GIÁ: " + productName + "\n");
        sb.append("--------------------------------------------------\n\n");

        if (auction.bidTransactions == null || auction.bidTransactions.isEmpty()) {
            sb.append("Chưa có lượt đặt giá nào.");
        } else {
            for (AuctionModel.BidTransactionModel tx : auction.bidTransactions) {
                String bidderName = tx.bidder != null ? tx.bidder.userName : "Unknown Bidder";
                sb.append("👤 ").append(bidderName)
                        .append("  👉  ").append(String.format("%,.0f VNĐ", tx.bidAmount)).append("\n");
            }
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Lịch sử Bid");
        alert.setHeaderText("Danh sách người tham gia đặt giá");

        // SỬA LỖI 3: DÙNG TEXTAREA ĐỂ HIỂN THỊ CHỮ RÕ RÀNG VÀ CÓ THANH CUỘN (KHI DANH SÁCH QUÁ DÀI)
        TextArea area = new TextArea(sb.toString());
        area.setWrapText(true);
        area.setEditable(false);
        area.setPrefSize(400, 250);

        alert.getDialogPane().setContent(area);

        // Gắn CSS Dark Mode & Nút viên thuốc
        AlertUtils.applyStyle(alert);

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
                    }
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
            private final Button btnBan = new Button("Khóa / Mở");
            private final Button btnDelete = new Button("Xóa");
            private final Button btnUpgrade = new Button("Up seller");
            private final HBox pane = new HBox(10, btnBan, btnDelete, btnUpgrade);

            {
                pane.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                btnBan.getStyleClass().addAll("btn-table", "btn-ban");
                btnDelete.getStyleClass().addAll("btn-table", "btn-delete");
                btnUpgrade.getStyleClass().addAll("btn-table", "btn-upgrade");

                btnBan.setOnAction(e -> toggleBanUser(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> deleteUser(getTableView().getItems().get(getIndex())));
                btnUpgrade.setOnAction(e -> upgradeUser(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
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
                    }
                }
            });
        });
    }

    private void toggleBanUser(UserModel user) {
        ApiService.putAsync("/users/admin/" + user.userName + "/ban", null).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() >= 200 && res.statusCode() < 300) {
                    loadUsers();
                }
            });
        });
    }

    private void deleteUser(UserModel user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Bạn chắc chắn muốn xóa người dùng " + user.userName + "?", ButtonType.YES, ButtonType.NO);
        AlertUtils.applyStyle(alert); // THÊM CSS

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                ApiService.deleteAsync("/users/admin/" + user.userName).thenAccept(res -> {
                    Platform.runLater(() -> {
                        if (res.statusCode() >= 200 && res.statusCode() < 300) loadUsers();
                    });
                });
            }
        });
    }

    private void upgradeUser(UserModel user) {
        ApiService.putAsync("/users/upgrade-to-seller/" + user.userName, null).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() >= 200 && res.statusCode() < 300) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Đã cấp quyền Seller cho " + user.userName + " thành công!");
                    AlertUtils.applyStyle(alert); // THÊM CSS
                    alert.show();
                    loadUsers();
                } else {
                    try {
                        ApiResponse errRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        Alert errAlert = new Alert(Alert.AlertType.ERROR, "Lỗi cấp quyền: " + errRes.message);
                        AlertUtils.applyStyle(errAlert); // THÊM CSS
                        errAlert.show();
                    } catch (Exception e) {
                        Alert errAlert = new Alert(Alert.AlertType.ERROR, "Lỗi cấp quyền: Mã lỗi " + res.statusCode());
                        AlertUtils.applyStyle(errAlert); // THÊM CSS
                        errAlert.show();
                    }
                }
            });
        });
    }

    @FXML
    private void goBack() {
        try {
            Node view = FXMLLoader.load(getClass().getResource("/com/auction/view/profile/Profile.fxml"));
            StackPane contentArea = (StackPane) tbItems.getScene().lookup("#contentArea");
            if(contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }
}